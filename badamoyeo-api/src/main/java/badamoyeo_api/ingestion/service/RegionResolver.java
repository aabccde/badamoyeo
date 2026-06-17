package badamoyeo_api.ingestion.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class RegionResolver {
	public String resolve(BigDecimal lat, BigDecimal lng) {
		if (lat == null || lng == null) {
			return null;
		}

		double latitude = lat.doubleValue();
		double longitude = lng.doubleValue();

		if (latitude >= 33.0 && latitude <= 34.1 && longitude >= 126.0 && longitude <= 127.2) {
			return "제주";
		}
		if (latitude >= 37.0 && latitude <= 39.0 && longitude >= 127.5 && longitude <= 129.8) {
			return "강원";
		}
		if (latitude >= 35.0 && latitude <= 36.7 && longitude >= 128.7 && longitude <= 129.6) {
			return "부산";
		}
		if (latitude >= 34.5 && latitude <= 35.7 && longitude >= 127.5 && longitude <= 129.4) {
			return "경남";
		}
		if (latitude >= 35.6 && latitude <= 37.2 && longitude >= 128.5 && longitude <= 130.0) {
			return "경북";
		}
		if (latitude >= 36.0 && latitude <= 37.2 && longitude >= 125.6 && longitude <= 127.7) {
			return "충남";
		}
		if (latitude >= 35.2 && latitude <= 36.2 && longitude >= 125.5 && longitude <= 127.3) {
			return "전북";
		}
		if (latitude >= 34.0 && latitude <= 35.4 && longitude >= 125.0 && longitude <= 127.8) {
			return "전남";
		}
		if (latitude >= 37.0 && latitude <= 38.2 && longitude >= 124.5 && longitude <= 126.9) {
			return "인천";
		}
		return null;
	}
}
