package net.osmand.binary;

import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;

public class BinaryMapDataObject {
	protected int[] coordinates = null;
	protected int[] types = null;
	
	protected int stringId = -1;
	protected long id = 0;
	
	protected long[] restrictions = null;
	protected int highwayAttributes = 0;
	
	protected String name;
	
	protected MapIndex mapIndex = null;
	
	
	public BinaryMapDataObject(){
	}
	
	protected void setStringId(int stringId) {
		this.stringId = stringId;
	}
	
	
	protected void setCoordinates(int[] coordinates) {
		this.coordinates = coordinates;
	}
	
	protected int getStringId() {
		return stringId;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public int[] getTypes(){
		return types;
	}
	
	public TagValuePair getTagValue(int indType){
		if(mapIndex == null){
			return null;
		}
		return mapIndex.decodeType(types[indType]);
	}
	
	public long getId() {
		return id;
	}
	
	protected void setId(long id) {
		this.id = id;
	}
	
	protected void setTypes(int[] types) {
		this.types = types;
	}
	
	
	public int getHighwayAttributes() {
		return highwayAttributes;
	}
	
	public MapIndex getMapIndex() {
		return mapIndex;
	}
	
	public void setMapIndex(MapIndex mapIndex) {
		this.mapIndex = mapIndex;
	}
	
	protected void setHighwayAttributes(int highwayAttributes) {
		this.highwayAttributes = highwayAttributes;
	}

	public int getPointsLength(){
		if(coordinates == null){
			return 0;
		}
		return coordinates.length / 2;
	}
	public int getPoint31YTile(int ind) {
		return coordinates[2 * ind + 1];
	}

	public int getPoint31XTile(int ind) {
		return coordinates[2 * ind];
	}
	
	public int getRestrictionCount(){
		if(restrictions == null){
			return 0;
		}
		return restrictions.length;
	}
	
	
	protected void setRestrictions(long[] restrictions) {
		this.restrictions = restrictions;
	}
	
	protected long[] getRestrictions() {
		return restrictions;
	}
	
	public byte getRestrictionType(int k){
		return (byte) (restrictions[k] & 7);
	}
	
	public long getRestriction(int k){
		long l = restrictions[k];
		return (l & ~7l) | (id & 7l);
	} 
	

}
