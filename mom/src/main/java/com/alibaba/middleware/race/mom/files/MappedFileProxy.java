package com.alibaba.middleware.race.mom.files;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月19日上午11:18:56
 *
 * @description 
 **/

public class MappedFileProxy {
	
	private MappedFile[] mappedFiles;
	public ConfigBuild configBuild;
	private MappedFileProxy(ConfigBuild configBuild) throws IOException{
		this.configBuild = configBuild;
		mappedFiles = new MappedFile[configBuild.getFileSize()];
		String home = configBuild.getHome() + configBuild.getPathPrefix();
		
		Path path = Paths.get(configBuild.getHome());
		if(!Files.isDirectory(path)){
			Files.createDirectories(path);
		}
			
		String filepath;
		for(int i= 0;i< configBuild.getFileSize();++i){
			filepath = home + i + configBuild.getPathSuffix(); 
			mappedFiles[i] = new MappedFile(filepath, configBuild.totalCapacity, configBuild.indexCapacity);
		}
	}
	public boolean putAndForce(int msgId,byte[] src){
		if( null == src|| src.length == 0){
			return false;
		}
		int fileIndex = msgId % configBuild.fileCount;
		mappedFiles[fileIndex].putAndForce(src);
		return true;
	}
	
	public byte[] get(int msgId){
		int fileIndex = msgId % configBuild.fileCount;
		int fileOffset = msgId / configBuild.fileCount;
		return mappedFiles[fileIndex].get(fileOffset);
	}
	
	public static final class ConfigBuild{
		private String home = "./";
		private String pathPrefix;
		private String pathSuffix;
		private int fileCount = Runtime.getRuntime().availableProcessors() * 2;
		int totalCapacity = 1024 * 1024 * 10;
		int indexCapacity = 1024 * 1024 * 1;
		
		public ConfigBuild() {
			// TODO 自动生成的构造函数存根
		}
		public ConfigBuild home(String home){
			this.home = home;
			return this;
		}
		public ConfigBuild pathPrefix(String pathPrefix){
			this.pathPrefix = pathPrefix;
			return this;
		}
		
		public ConfigBuild pathSuffix(String pathSuffix){
			this.pathSuffix = pathSuffix;
			return this;
		}
		
		public ConfigBuild fileCount(int fileCount){
			this.fileCount = fileCount;
			return this;
		}
		
		public ConfigBuild totalCapacity(int totalCapacity){
			this.totalCapacity = totalCapacity;
			return this;
		}
		
		public ConfigBuild indexCapacity(int indexCapacity){
			this.indexCapacity = indexCapacity;
			return this;
		}
		
		public MappedFileProxy build() throws IOException{
			return new MappedFileProxy(this);
		}
		
		public String getHome() {
			return home;
		}
		public String getPathPrefix() {
			return pathPrefix;
		}
		public String getPathSuffix() {
			return pathSuffix;
		}
		public int getFileSize() {
			return fileCount;
		}
	}
	public static void main(String[] args) throws IOException {
		String home = System.getProperty("user.home") + "/store/";
		String prefix = "data";
		String suffix = ".txt";
		MappedFileProxy proxy = new MappedFileProxy
				.ConfigBuild()
				.home("./store/")
				.pathPrefix(prefix)
				.pathSuffix(suffix)
				.totalCapacity(1024 * 1024)
				.indexCapacity(1024)
				.fileCount(10)
				.build();
		long st = System.currentTimeMillis();
		for(int i = 0;i < 100;i++){
			proxy.putAndForce(i, ("helloworld" + i).getBytes());
		}
		System.out.println(System.currentTimeMillis() - st);
		st = System.currentTimeMillis();
		Random random = new Random();
		int index; 
		for(int i=0;i<100;i++){
			System.out.println(new String(proxy.get(i)));
		}
		System.out.println(System.currentTimeMillis() - st);
	}
}
