package wseemann.media.romote.model;

public class Channel {

	private String mId;
	private String mTitle;
	private String mType;
	private String mVersion;
	private String mImageUrl; 
	
	public Channel() {
		
	}

	
	public Channel(String id, String title, String type, String version, String imageUrl) {
		this.mId = id;
		this.mTitle = title;
		this.mType = type;
		this.mVersion = version;
		this.mImageUrl = imageUrl;
	}

	public String getId() {
		return mId;
	}

	public void setId(String mId) {
		this.mId = mId;
	}

	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String mTitle) {
		this.mTitle = mTitle;
	}

	public String getType() {
		return mType;
	}

	public void setType(String mType) {
		this.mType = mType;
	}
	
	public String getVerion() {
		return mVersion;
	}

	public void setVersion(String mVersion) {
		this.mVersion = mVersion;
	}
	
	public String getImageUrl() {
		return mImageUrl;
	}

	public void setImageUrl(String mImageUrl) {
		this.mImageUrl = mImageUrl;
	}
	
	@Override
	public String toString() {
		return mId + " " + mTitle + " " + mType + " " + mVersion + " " + mImageUrl;
	}
}
