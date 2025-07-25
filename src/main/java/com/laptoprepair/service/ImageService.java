package com.laptoprepair.service;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ImageService {
    

    List<RequestImage> deleteImages(UUID requestId, List<RequestImage> currentImages, String[] toDelete) 
            throws ValidationException;
            
    List<RequestImage> uploadImages(UUID requestId, List<RequestImage> currentImages, MultipartFile[] newImages, Request request) 
            throws ValidationException;
            
    List<RequestImage> updateRequestServiceImages(Request request, MultipartFile[] newImages, String[] toDelete) 
            throws ValidationException;
}