package tyt.android.bigplanettracks.maps.db;

import java.io.Serializable;

import tyt.android.bigplanettracks.maps.RawTile;

public class GeoBookmark implements Serializable {
	
	private static final long serialVersionUID = -2198154484982426107L;

	private int id = -1;
	
    private String name;
	
	private String description;
	
	private RawTile tile;
	
	private int offsetX;
	
	private int offsetY;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public RawTile getTile(){
		return this.tile;
	}

	public void setTile(RawTile tile){
		this.tile = tile;
	}
	
	public int getOffsetX() {
		return offsetX;
	}

	public void setOffsetX(int offsetX) {
		this.offsetX = offsetX;
	}

	public int getOffsetY() {
		return offsetY;
	}

	public void setOffsetY(int offsetY) {
		this.offsetY = offsetY;
	}
}
