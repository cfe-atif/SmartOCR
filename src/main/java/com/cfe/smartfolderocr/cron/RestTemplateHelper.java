package com.cfe.smartfolderocr.cron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Component
public class RestTemplateHelper {

    private RestTemplate restTemplate;

    private Logger logger = LoggerFactory.getLogger(RestTemplateHelper.class);

    public RestTemplateHelper()
    {
        restTemplate = new RestTemplate();
    }

    public HttpEntity<String> callUrlWithSession(String url, String sessionId, HttpHeaders headers, String fileText, String xDoc) {
        logger.info(url);
        ResponseEntity<String> response = null;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, prepareEntity(sessionId, headers, fileText, true, false), String.class);
            if(response.getStatusCode().value() != 200) {
                logger.error("Unable to process document number {} in SmartFolders", xDoc);
            }
        }
        catch (Exception e)
        {
            logger.error("Error in callUrlWithSession: message = {}", e.getMessage(), e);
        }
        logger.debug("response: " + response.getBody());
        return response;
    }

    public ResponseEntity<byte[]> callUrlWithSessionAndDownloadFile(String url, String sessionId, HttpHeaders headers) {
        logger.info(url);

        ResponseEntity<byte[]> response = null;
        try{
            response = restTemplate.exchange(url, HttpMethod.POST, prepareEntity(sessionId, headers, null, false, true), byte[].class);
        }
        catch (Exception e)
        {
            logger.error("Error in callUrlWithSessionAndDownloadFile: message = " + e.getMessage());
        }
        return response;
    }

    private HttpEntity prepareEntity(String sessionId, HttpHeaders headers, String fileText, boolean isSetContentTypeText, boolean isSetAcceptOctetStream) {
        return new HttpEntity(fileText, prepareHeaders(sessionId, headers, isSetContentTypeText, isSetAcceptOctetStream));
    }

    private HttpHeaders prepareHeaders(String sessionId, HttpHeaders headers, boolean isSetContentTypeText, boolean isSetAcceptOctetStream) {
        if (headers != null) {
            headers.add("Cookie", sessionId);
            if (isSetContentTypeText)
                headers.setContentType(MediaType.TEXT_PLAIN);
            if(isSetAcceptOctetStream)
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
        }
        return headers;
    }
}
