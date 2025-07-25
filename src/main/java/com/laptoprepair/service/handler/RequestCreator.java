package com.laptoprepair.service.handler;

import com.laptoprepair.entity.Request;
import com.laptoprepair.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

public interface RequestCreator {
    Request createNew(Request request, MultipartFile[] images, String note) throws ValidationException;
    Request createPublic(Request request) throws ValidationException;
}