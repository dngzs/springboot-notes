package com.best.controller.upload;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

/**
 * 文件上传
 *
 * @author dngzs
 * @date 2019-05-14 20:49
 */
@Controller
public class UploadController {

    @RequestMapping("/upload1")
    @ResponseBody
    public String upload1(@RequestParam("file") CommonsMultipartFile file, HttpServletRequest request) {
        if (!file.isEmpty()) {
            OutputStream outputStream = null;
            InputStream inputStream = null;
            try {
                if (file.isEmpty()) {
                    throw new RuntimeException("error");
                }
                inputStream = file.getInputStream();
                outputStream = new FileOutputStream("E://2.txt");
                byte[] buff = new byte[1024];
                int byteRead;
                while ((byteRead = inputStream.read(buff)) != -1) {
                    outputStream.write(buff, 0, byteRead);
                }
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    outputStream.close();
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "上传成功";
    }

    @RequestMapping("/upload2")
    @ResponseBody
    public String upload2(HttpServletRequest request) {
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(request.getSession().getServletContext());
        if (multipartResolver.isMultipart(request)) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            MultiValueMap<String, MultipartFile> multiFileMap = multipartRequest.getMultiFileMap();
            Collection<List<MultipartFile>> values = multiFileMap.values();
            values.forEach(multipartFiles -> {
                if (CollectionUtils.isNotEmpty(multipartFiles)) {
                    multipartFiles.forEach(multipartFile -> {
                        OutputStream outputStream = null;
                        InputStream inputStream = null;
                        try {
                            if (multipartFile.isEmpty()) {
                                throw new RuntimeException("error");
                            }
                            inputStream = multipartFile.getInputStream();
                            outputStream = new FileOutputStream("E://1.txt");
                            byte[] buff = new byte[1024];
                            int byteRead;
                            while ((byteRead = inputStream.read(buff)) != -1) {
                                outputStream.write(buff, 0, byteRead);
                            }
                            outputStream.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                                if (outputStream != null) {
                                    inputStream.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
        }
        return "上传成功";
    }
}
