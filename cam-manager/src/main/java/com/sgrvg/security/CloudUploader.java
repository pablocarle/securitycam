package com.sgrvg.security;

import java.io.InputStream;

public interface CloudUploader {

	void setBasePath(String basePath);
	
	void upload(InputStream is);
	
	void delete(String fileName);
	
}
