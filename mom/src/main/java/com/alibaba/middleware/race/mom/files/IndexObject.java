package com.alibaba.middleware.race.mom.files;

/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月17日上午11:04:23
 *
 * @description 
 **/

public class IndexObject {

	private int position;
	private short length;
	private byte isDone;
	private byte ext;
	public IndexObject(int position,short length){
		this.position = position;
		this.length = length;
		this.isDone = 0;
		this.ext = 0;
	}
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public short getLength() {
		return length;
	}
	public void setLength(short length) {
		this.length = length;
	}
	public byte getIsDone() {
		return isDone;
	}
	public void setIsDone(byte isDone) {
		this.isDone = isDone;
	}
	public byte getExt() {
		return ext;
	}
	public void setExt(byte ext) {
		this.ext = ext;
	}
	public String toString(){
		return "pos:" + position + " length:" + length;
	}
}
  