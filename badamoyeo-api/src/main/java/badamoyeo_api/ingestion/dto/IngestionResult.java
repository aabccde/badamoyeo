package badamoyeo_api.ingestion.dto;

public record IngestionResult(
	String experience,
	int fetchedCount,
	int savedCount
) {
}
