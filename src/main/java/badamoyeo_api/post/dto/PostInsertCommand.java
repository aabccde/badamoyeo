package badamoyeo_api.post.dto;

public class PostInsertCommand {
	private Long id;
	private Long spotId;
	private Long userId;
	private String title;
	private String content;

	public PostInsertCommand(Long spotId, Long userId, String title, String content) {
		this.spotId = spotId;
		this.userId = userId;
		this.title = title;
		this.content = content;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSpotId() {
		return spotId;
	}

	public Long getUserId() {
		return userId;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}
}
