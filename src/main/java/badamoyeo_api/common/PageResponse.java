package badamoyeo_api.common;

import java.util.List;

public record PageResponse<T>(
	List<T> items,
	int page,
	int pageSize,
	long totalCount,
	boolean hasNext
) {
	public static <T> PageResponse<T> of(List<T> items, int page, int pageSize, long totalCount) {
		return new PageResponse<>(items, page, pageSize, totalCount, (long) page * pageSize < totalCount);
	}
}
