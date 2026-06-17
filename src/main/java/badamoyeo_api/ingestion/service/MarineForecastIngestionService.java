package badamoyeo_api.ingestion.service;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import badamoyeo_api.ingestion.dto.ForecastUpsertRequest;
import badamoyeo_api.ingestion.dto.IngestionResult;
import badamoyeo_api.ingestion.dto.SpotIdLookup;
import badamoyeo_api.ingestion.dto.SpotUpsertRequest;
import badamoyeo_api.ingestion.mapper.MarineForecastIngestionMapper;
import badamoyeo_api.spot.dto.Experience;

@Service
public class MarineForecastIngestionService {
	private static final Logger log = LoggerFactory.getLogger(MarineForecastIngestionService.class);
	private static final int[] PAGE_SIZES = {300, 100, 50, 10};
	private static final int MAX_FETCH_ATTEMPTS = 3;
	private static final long FETCH_RETRY_DELAY_MILLIS = 500;
	private static final DateTimeFormatter REQUEST_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

	private final MarineForecastIngestionMapper ingestionMapper;
	private final RegionResolver regionResolver;
	private final TransactionTemplate transactionTemplate;
	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String serviceKey;

	public MarineForecastIngestionService(
		MarineForecastIngestionMapper ingestionMapper,
		RegionResolver regionResolver,
		TransactionTemplate transactionTemplate,
		ObjectMapper objectMapper,
		@Value("${openapi.marine.service-key:}") String serviceKey
	) {
		this.ingestionMapper = ingestionMapper;
		this.regionResolver = regionResolver;
		this.transactionTemplate = transactionTemplate;
		this.objectMapper = objectMapper;
		this.serviceKey = serviceKey;
		this.restClient = RestClient.create();
	}

	public List<IngestionResult> ingestAll(LocalDate targetDate) {
		if (serviceKey == null || serviceKey.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "openapi.marine.service-key is required");
		}

		LocalDate requestDate = targetDate == null ? LocalDate.now() : targetDate;
		List<IngestionResult> results = new ArrayList<>();
		for (ApiSpec spec : apiSpecs()) {
			try {
				results.add(ingest(spec, requestDate));
			} catch (ResponseStatusException exception) {
				log.warn("Skip marine forecast experience. experience={}, date={}, status={}, reason={}",
					spec.experience().apiValue(),
					requestDate,
					exception.getStatusCode(),
					exception.getReason());
			}
		}
		return results;
	}

	private List<ApiSpec> apiSpecs() {
		return List.of(
			ApiSpec.seaTravel(),
			ApiSpec.swimming(),
			ApiSpec.fishing(),
			ApiSpec.mudflat(),
			ApiSpec.scuba(),
			ApiSpec.surfing()
		);
	}

	private IngestionResult ingest(ApiSpec spec, LocalDate requestDate) {
		int pageNo = 1;
		int totalCount = 0;
		int fetchedCount = 0;
		int savedCount = 0;

		do {
			FetchPage page = fetchPageWithFallback(spec, requestDate, pageNo, PAGE_SIZES[0], totalCount, 0);
			List<JsonNode> items = page.items();
			totalCount = page.totalCount();

			fetchedCount += items.size();
			savedCount += saveItems(spec, items);

			pageNo++;
		} while ((pageNo - 1) * PAGE_SIZES[0] < totalCount);

		return new IngestionResult(spec.experience().apiValue(), fetchedCount, savedCount);
	}

	private FetchPage fetchPageWithFallback(ApiSpec spec, LocalDate requestDate, int pageNo, int pageSize,
		int knownTotalCount, int pageSizeIndex) {
		try {
			return fetchPage(spec, requestDate, pageNo, pageSize);
		} catch (ResponseStatusException exception) {
			if (!shouldRetry(exception) || pageSizeIndex >= PAGE_SIZES.length - 1) {
				throw exception;
			}
			int fallbackPageSize = PAGE_SIZES[pageSizeIndex + 1];
			log.warn("Fallback to smaller marine forecast pages. experience={}, date={}, pageNo={}, numOfRows={}, fallbackNumOfRows={}, status={}, reason={}",
				spec.experience().apiValue(),
				requestDate,
				pageNo,
				pageSize,
				fallbackPageSize,
				exception.getStatusCode(),
				exception.getReason());
			return fetchFallbackPage(spec, requestDate, pageNo, pageSize, fallbackPageSize, knownTotalCount, pageSizeIndex + 1);
		}
	}

	private FetchPage fetchFallbackPage(ApiSpec spec, LocalDate requestDate, int primaryPageNo, int primaryPageSize,
		int fallbackPageSize, int knownTotalCount, int fallbackPageSizeIndex) {
		int firstRow = (primaryPageNo - 1) * primaryPageSize + 1;
		int lastRow = knownTotalCount > 0 ? Math.min(primaryPageNo * primaryPageSize, knownTotalCount) : primaryPageNo * primaryPageSize;
		int firstFallbackPageNo = (firstRow - 1) / fallbackPageSize + 1;
		int lastFallbackPageNo = (lastRow - 1) / fallbackPageSize + 1;

		List<JsonNode> items = new ArrayList<>();
		int totalCount = knownTotalCount;
		for (int fallbackPageNo = firstFallbackPageNo; fallbackPageNo <= lastFallbackPageNo; fallbackPageNo++) {
			FetchPage fallbackPage = fetchPageWithFallback(spec, requestDate, fallbackPageNo, fallbackPageSize, totalCount, fallbackPageSizeIndex);
			totalCount = fallbackPage.totalCount();
			items.addAll(fallbackPage.items());
			if (fallbackPageNo == firstFallbackPageNo && knownTotalCount == 0) {
				lastRow = Math.min(primaryPageNo * primaryPageSize, totalCount);
				lastFallbackPageNo = (lastRow - 1) / fallbackPageSize + 1;
			}
		}

		log.info("Fetched marine forecast page by fallback. experience={}, date={}, pageNo={}, numOfRows={}, fallbackNumOfRows={}, totalCount={}, itemCount={}",
			spec.experience().apiValue(),
			requestDate,
			primaryPageNo,
			primaryPageSize,
			fallbackPageSize,
			totalCount,
			items.size());
		return new FetchPage(items, totalCount);
	}

	private FetchPage fetchPage(ApiSpec spec, LocalDate requestDate, int pageNo, int pageSize) {
		for (int attempt = 1; attempt <= MAX_FETCH_ATTEMPTS; attempt++) {
			try {
				FetchPage page = fetchPageOnce(spec, requestDate, pageNo, pageSize);
				log.info("Fetched marine forecast page. experience={}, date={}, pageNo={}, numOfRows={}, totalCount={}, itemCount={}",
					spec.experience().apiValue(),
					requestDate,
					pageNo,
					pageSize,
					page.totalCount(),
					page.items().size());
				return page;
			} catch (ResponseStatusException exception) {
				if (attempt == MAX_FETCH_ATTEMPTS || !shouldRetry(exception)) {
					throw exception;
				}
				log.warn("Retry marine forecast page. experience={}, date={}, pageNo={}, numOfRows={}, attempt={}, status={}, reason={}",
					spec.experience().apiValue(),
					requestDate,
					pageNo,
					pageSize,
					attempt,
					exception.getStatusCode(),
					exception.getReason());
				sleepBeforeRetry();
			}
		}
		throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to fetch open api response");
	}

	private FetchPage fetchPageOnce(ApiSpec spec, LocalDate requestDate, int pageNo, int pageSize) {
		URI uri = forecastUri(spec, requestDate, pageNo, pageSize);

		String responseBody;
		try {
			responseBody = restClient.get()
				.uri(uri)
				.retrieve()
				.body(String.class);
		} catch (RestClientResponseException exception) {
			throw new ResponseStatusException(exception.getStatusCode(),
				"open api http error: experience=" + spec.experience().apiValue()
					+ ", date=" + requestDate
					+ ", pageNo=" + pageNo
					+ ", status=" + exception.getStatusCode()
					+ ", body=" + sanitizeResponseBody(exception.getResponseBodyAsString()),
				exception);
		}

		if (responseBody == null || responseBody.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "empty open api response");
		}

		JsonNode root = parseJson(responseBody);
		JsonNode envelope = root.has("response") ? root.path("response") : root;
		JsonNode header = envelope.path("header");
		String resultCode = header.path("resultCode").asText();
		if (!"00".equals(resultCode)) {
			String resultMsg = header.path("resultMsg").asText("open api request failed");
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
				"open api request failed: experience=" + spec.experience().apiValue()
					+ ", date=" + requestDate
					+ ", pageNo=" + pageNo
					+ ", resultCode=" + resultCode
					+ ", resultMsg=" + resultMsg);
		}

		JsonNode body = envelope.path("body");
		return new FetchPage(asArray(body.path("items").path("item")), body.path("totalCount").asInt(0));
	}

	private boolean shouldRetry(ResponseStatusException exception) {
		if (exception.getStatusCode().is5xxServerError()) {
			return true;
		}
		return exception.getReason() != null && exception.getReason().contains("resultCode=99");
	}

	private void sleepBeforeRetry() {
		try {
			Thread.sleep(FETCH_RETRY_DELAY_MILLIS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "interrupted while retrying open api request", exception);
		}
	}

	private URI forecastUri(ApiSpec spec, LocalDate requestDate, int pageNo, int pageSize) {
		String query = UriComponentsBuilder.newInstance()
			.queryParam("type", "json")
			.queryParam("reqDate", requestDate.format(REQUEST_DATE_FORMATTER))
			.queryParam("pageNo", pageNo)
			.queryParam("numOfRows", pageSize)
			.queryParams(spec.fixedParams())
			.build()
			.encode()
			.getQuery();
		String serviceKeyQuery = "serviceKey=" + encodeServiceKey(serviceKey);
		return URI.create("https://apis.data.go.kr/1192136/" + spec.path()
			+ "?" + serviceKeyQuery
			+ (query == null || query.isBlank() ? "" : "&" + query));
	}

	private String encodeServiceKey(String value) {
		if (value.contains("%")) {
			return value;
		}
		return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
	}

	private String sanitizeResponseBody(String body) {
		if (body == null || body.isBlank()) {
			return "";
		}
		String compact = body.replaceAll("\\s+", " ").trim();
		return compact.length() > 200 ? compact.substring(0, 200) : compact;
	}

	private JsonNode parseJson(String responseBody) {
		try {
			return objectMapper.readTree(responseBody);
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid open api response", exception);
		}
	}

	private List<JsonNode> asArray(JsonNode items) {
		if (items == null || items.isMissingNode() || items.isNull()) {
			return List.of();
		}
		if (items.isArray()) {
			List<JsonNode> nodes = new ArrayList<>();
			items.forEach(nodes::add);
			return nodes;
		}
		return List.of(items);
	}

	private int saveItems(ApiSpec spec, List<JsonNode> items) {
		return transactionTemplate.execute(status -> saveItemsInTransaction(spec, items));
	}

	private int saveItemsInTransaction(ApiSpec spec, List<JsonNode> items) {
		if (items.isEmpty()) {
			return 0;
		}

		Map<String, SpotUpsertRequest> uniqueSpots = new LinkedHashMap<>();
		for (JsonNode item : items) {
			SpotUpsertRequest spotRequest = toSpotRequest(spec, item);
			uniqueSpots.put(spotKey(spotRequest.experience(), spotRequest.name()), spotRequest);
		}

		List<SpotUpsertRequest> spots = new ArrayList<>(uniqueSpots.values());
		ingestionMapper.upsertSpots(spots);

		Map<String, Long> spotIds = new LinkedHashMap<>();
		for (SpotIdLookup lookup : ingestionMapper.findSpotIds(spots)) {
			spotIds.put(spotKey(lookup.experience(), lookup.name()), lookup.spotId());
		}

		List<ForecastUpsertRequest> forecasts = new ArrayList<>();
		for (JsonNode item : items) {
			SpotUpsertRequest spotRequest = toSpotRequest(spec, item);
			Long spotId = spotIds.get(spotKey(spotRequest.experience(), spotRequest.name()));
			if (spotId == null) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to resolve spot id");
			}
			forecasts.add(toForecastRequest(spec, item, spotId));
		}

		ingestionMapper.upsertForecasts(forecasts);
		return forecasts.size();
	}

	private SpotUpsertRequest toSpotRequest(ApiSpec spec, JsonNode item) {
		String spotName = text(item, spec.nameField());
		BigDecimal lat = decimal(item, "lat");
		BigDecimal lng = decimal(item, "lot");

		return new SpotUpsertRequest(
			spec.experience().apiValue(),
			spotName,
			lat,
			lng,
			spec.placeCode(item),
			spotName,
			regionResolver.resolve(lat, lng)
		);
	}

	private ForecastUpsertRequest toForecastRequest(ApiSpec spec, JsonNode item, Long spotId) {
		LocalDate forecastDate = LocalDate.parse(text(item, "predcYmd"));
		String timeSlot = nullToEmpty(text(item, "predcNoonSeCd"));
		String totalIndex = text(item, "totalIndex");
		String weather = text(item, "weather");
		String tide = text(item, "tdlvHrCn");
		String variantKey = spec.variantKey(item);

		return new ForecastUpsertRequest(
			spotId,
			spec.experience().apiValue(),
			forecastDate,
			timeSlot,
			totalIndex,
			weather,
			tide,
			variantKey,
			toJson(spec.metrics(item)),
			toJson(item)
		);
	}

	private String spotKey(String experience, String name) {
		return experience + "\n" + name;
	}

	private String toJson(JsonNode node) {
		try {
			return objectMapper.writeValueAsString(node);
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to serialize json", exception);
		}
	}

	private String text(JsonNode item, String field) {
		JsonNode value = item.path(field);
		if (value.isMissingNode() || value.isNull()) {
			return null;
		}
		String text = value.asText();
		return text.isBlank() ? null : text;
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private BigDecimal decimal(JsonNode item, String field) {
		String value = text(item, field);
		return value == null ? null : new BigDecimal(value);
	}

	private record ApiSpec(
		Experience experience,
		String path,
		String nameField,
		org.springframework.util.MultiValueMap<String, String> fixedParams,
		List<MetricField> metrics,
		String variantField
	) {
		static ApiSpec seaTravel() {
			return new ApiSpec(Experience.SEA_TRAVEL, "fcstSeaTripv2/GetFcstSeaTripApiServicev2", "sareaDtlNm",
				new org.springframework.util.LinkedMultiValueMap<>(),
				List.of(
					new MetricField("weather", "weather"),
					new MetricField("tide", "tdlvHrCn"),
					new MetricField("airTemperature", "avgArtmp"),
					new MetricField("windSpeed", "avgWspd"),
					new MetricField("waterTemperature", "avgWtem"),
					new MetricField("waveHeight", "avgWvhgt"),
					new MetricField("currentSpeed", "avgCrsp")
				),
				null
			);
		}

		static ApiSpec swimming() {
			return new ApiSpec(Experience.SWIMMING, "fcstBeachv2/GetFcstBeachApiServicev2", "bbchNm",
				new org.springframework.util.LinkedMultiValueMap<>(),
				List.of(
					new MetricField("openStatus", "opnStat"),
					new MetricField("waterTemperature", "avgWtem"),
					new MetricField("waveHeight", "maxWvhgt"),
					new MetricField("airTemperature", "avgArtmp"),
					new MetricField("windSpeed", "maxWspd")
				),
				null
			);
		}

		static ApiSpec fishing() {
			org.springframework.util.LinkedMultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
			params.add("gubun", "갯바위");
			return new ApiSpec(Experience.FISHING, "fcstFishingv2/GetFcstFishingApiServicev2", "seafsPstnNm",
				params,
				List.of(
					new MetricField("targetFish", "seafsTgfshNm"),
					new MetricField("tide", "tdlvHrCn"),
					new MetricField("waterTemperatureMin", "minWtem"),
					new MetricField("waterTemperatureMax", "maxWtem"),
					new MetricField("waveHeightMin", "minWvhgt"),
					new MetricField("waveHeightMax", "maxWvhgt"),
					new MetricField("airTemperatureMin", "minArtmp"),
					new MetricField("airTemperatureMax", "maxArtmp"),
					new MetricField("currentSpeedMin", "minCrsp"),
					new MetricField("currentSpeedMax", "maxCrsp"),
					new MetricField("windSpeedMin", "minWspd"),
					new MetricField("windSpeedMax", "maxWspd")
				),
				"seafsTgfshNm"
			);
		}

		static ApiSpec mudflat() {
			return new ApiSpec(Experience.MUDFLAT, "fcstMudflatv2/GetFcstMudflatApiServicev2", "mdftExpcnVlgNm",
				new org.springframework.util.LinkedMultiValueMap<>(),
				List.of(
					new MetricField("weather", "weather"),
					new MetricField("availableStartTime", "mdftExprnBgngTm"),
					new MetricField("availableEndTime", "mdftExprnEndTm"),
					new MetricField("airTemperatureMin", "minArtmp"),
					new MetricField("airTemperatureMax", "maxArtmp"),
					new MetricField("windSpeedMin", "minWspd"),
					new MetricField("windSpeedMax", "maxWspd")
				),
				null
			);
		}

		static ApiSpec scuba() {
			return new ApiSpec(Experience.SCUBA, "fcstSkinScubav2/GetFcstSkinScubaApiServicev2", "skscExpcnRgnNm",
				new org.springframework.util.LinkedMultiValueMap<>(),
				List.of(
					new MetricField("tideStage", "tdlvHrCn"),
					new MetricField("waterTemperatureMin", "minWtem"),
					new MetricField("waterTemperatureMax", "maxWtem"),
					new MetricField("waveHeightMin", "minWvhgt"),
					new MetricField("waveHeightMax", "maxWvhgt"),
					new MetricField("currentSpeedMin", "minCrsp"),
					new MetricField("currentSpeedMax", "maxCrsp")
				),
				null
			);
		}

		static ApiSpec surfing() {
			return new ApiSpec(Experience.SURFING, "fcstSurfingv2/GetFcstSurfingApiServicev2", "surfPlcNm",
				new org.springframework.util.LinkedMultiValueMap<>(),
				List.of(
					new MetricField("level", "grdCn"),
					new MetricField("waveHeight", "avgWvhgt"),
					new MetricField("wavePeriod", "avgWvpd"),
					new MetricField("windSpeed", "avgWspd"),
					new MetricField("waterTemperature", "avgWtem")
				),
				null
			);
		}

		String placeCode(JsonNode item) {
			return experience.apiValue() + ":" + item.path(nameField).asText();
		}

		String variantKey(JsonNode item) {
			return variantField == null ? "" : item.path(variantField).asText("");
		}

		ObjectNode metrics(JsonNode item) {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode node = mapper.createObjectNode();
			for (MetricField metric : metrics) {
				JsonNode value = item.path(metric.sourceField());
				if (!value.isMissingNode() && !value.isNull()) {
					node.set(metric.apiField(), value);
				}
			}
			return node;
		}
	}

	private record MetricField(String apiField, String sourceField) {
	}

	private record FetchPage(List<JsonNode> items, int totalCount) {
	}
}
