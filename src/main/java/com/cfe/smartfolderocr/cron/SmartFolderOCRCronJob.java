/*
 * Copyright (c) Shah Pvt Limited 2020. All rights reserved.
 * This source code is confidential to and the copyright of Shah Pvt Limited ("Shah Pvt"), and must not be
 * (i) copied, shared, reproduced in whole or in part; or
 * (ii) used for any purpose other than the purpose for which it has expressly been provided by Shah Pvt under the terms of a license agreement; or
 * (iii) given or communicated to any third party without the prior written consent of Shah Pvt.
 * Shah Pvt at all times reserves the right to modify the delivery and capabilities of its products and/or services.
 * "Shah" and "Shah Pvt" are registered trademarks of Shah Pvt.
 * All other brands and logos (that are not registered and/or unregistered trademarks of Shah Pvt) are registered and/or
 * unregistered trademarks of their respective holders and should be treated as such.
 */
package com.cfe.smartfolderocr.cron;

import com.cfe.smartfolderocr.gson.Database;
import com.cfe.smartfolderocr.gson.GetNextDocumentDTO;
import com.cfe.smartfolderocr.gson.User;
import com.google.gson.Gson;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class SmartFolderOCRCronJob {

    @Value("${error.user.not.initialized:User and Databases not initialized. Please check user credentials and server url}")
    private String USER_NOT_INITIALIZED;

    @Value("${smart.folder.server:http://127.0.0.1}")
    private String SERVER_URL;

    @Value("${smart.folder.server.servlet.json:http://127.0.0.1/servlets/servlets.CH_VaultJson?}")
    private String SERVER_URL_JSON;

    @Value("${smart.folder.server.login:INT=1&}")
    private String STEP_1_LOGIN_URL;

    @Value("${smart.folder.server.login.user.name:Name=sysadmin&}")
    private String loginName;
    @Value("${smart.folder.server.login.user.password:Password=password}")
    private String loginPassword;

    private Logger logger = LoggerFactory.getLogger(SmartFolderOCRCronJob.class);
    private RestTemplate restTemplate;

    private User user;
    private String sessionId;
    private Tika tika;

    public SmartFolderOCRCronJob() {
        restTemplate = new RestTemplate();
    }

    // schedule this job to run after x minutes
    @Scheduled(fixedRateString = "${smart.folder.job.delay:300000}", initialDelayString = "${smart.folder.job.delay:300000}")
   // @Scheduled(fixedDelay = 300000, initialDelay = 300000)
    public void runCronJob() throws TikaException, IOException {
        if (!isUserDataInitialized())
            initializeSessionIdAfterSignin();
        lookForDocumentsAndProcessForOcr();
    }

    private void initializeSessionIdAfterSignin() {
        logger.info("Initializing session and sign in ");
        HttpEntity<String> response = callUrlWithSession(SERVER_URL_JSON + STEP_1_LOGIN_URL + loginName + loginPassword, "", null);
        String resultString = response.getBody();
        HttpHeaders headers = response.getHeaders();
        logger.info("response " + resultString);

        if (resultString.contains("Error")) {
            logger.error(USER_NOT_INITIALIZED);
            return;
        } else {
            user = new Gson().fromJson(resultString, User.class);
            logger.info("after gson conversion " + user.toString());
            sessionId = Arrays.stream(headers.get("Set-Cookie").get(0).split(";")).filter(word -> word.contains("JSESSIONID=")).findFirst().get();
        }
    }

    private HttpEntity<String> callUrlWithSession(String url, String sessionId, HttpHeaders headers) {
        if (headers != null) {
            headers.add("Cookie", sessionId);
        }
        HttpEntity entity = new HttpEntity(null, headers);
        logger.info(url);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    private HttpEntity<String> callUrlWithSessionAndUploadFile(String url, String sessionId, HttpHeaders headers, String fileText) {
        if (headers != null) {
            headers.add("Cookie", sessionId);
            headers.setContentType(MediaType.TEXT_PLAIN);
        }
        HttpEntity entity = new HttpEntity(fileText, headers);
        logger.info(url);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    private ResponseEntity<byte[]> callUrlWithSessionAndDownloadFile(String url, String sessionId, HttpHeaders headers) {
        if (headers != null) {
            headers.add("Cookie", sessionId);
            headers.setContentType(MediaType.TEXT_PLAIN);
        }
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
        HttpEntity entity = new HttpEntity(null, headers);
        logger.info(url);
        return restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
    }

    public void lookForDocumentsAndProcessForOcr() throws IOException, TikaException {
        logger.info("look For Documents And Process For Ocr started");

        if (!isUserDataInitialized()) {
            logger.info("User data is not initialized. Returning");
            return;
        }

        if(!CollectionUtils.isEmpty(user.databases)) {
            logger.info("Processing multiple DBs");
            for (int i = 0; i < user.databases.size(); i++) {
                processDatabaseForDocumentOcr(user.databases.get(i));
            }
        }

        logger.info("lookForDocumentsAndProcessForOcr ended");
    }

    private void processDatabaseForDocumentOcr(Database currentDatabase) throws IOException, TikaException {
        logger.debug("looking in database " + currentDatabase);

        String xDB = (CronHelper.createXML("Database", "" + currentDatabase.dbNo));
        String sUser = (CronHelper.createXML("User", "" + user.userData.userNo));

        //call to get next doc to OCR
        String nextDocumentToOCRResponse = getNextDocumentToOCRResponse(xDB, sUser);

        if (nextDocumentToOCRResponse.contains("session") || nextDocumentToOCRResponse.contains("Session")) {
            initializeSessionIdAfterSignin();
        }

        GetNextDocumentDTO nextDocumentDTO = new GetNextDocumentDTO(nextDocumentToOCRResponse);

        while ((nextDocumentDTO.docNo != null) && (!nextDocumentDTO.docNo.equals("null"))) {

            String xDoc = (CronHelper.createXML("Document", "" + nextDocumentDTO.docNo));
            String xPage = (CronHelper.createXML("Page", "" + nextDocumentDTO.pageNo));
            logger.debug(nextDocumentDTO.toString());

            //check for null
            String extractedText = extractTextFromFileFromServer(xDB, sUser, xDoc, xPage);
            if (extractedText != null) {
                String docType = (CronHelper.createXML("DocType", "1"));
                String sendPageTextMessage = CronHelper.createXML("m:CH_AddPageText", sUser + xDB + docType + xDoc + xPage);
                HttpEntity<String> httpEntity = callUrlWithSessionAndUploadFile(getStringForSoapCallWithMessage(sendPageTextMessage), sessionId, new HttpHeaders(), extractedText);
                //logger.info(httpEntity.getBody());
            }

            //call to get next doc to OCR
            nextDocumentToOCRResponse = getNextDocumentToOCRResponse(xDB, sUser);

            nextDocumentDTO = new GetNextDocumentDTO(nextDocumentToOCRResponse);
        }
    }

    private boolean isUserDataInitialized() {
        logger.info("isUserDataInitialized user: " + user + " sessionId: "+sessionId);
        return user != null && !CollectionUtils.isEmpty(user.databases) && !StringUtils.isBlank(sessionId);
    }

    private String extractTextFromFileFromServer(String xDB, String sUser, String xDoc, String xPage) throws IOException, TikaException {
        String sMessage = "<m:ReadImageOCR>" + sUser + xDB + xDoc + xPage + "<OCR>true</OCR><AsStream>true</AsStream></m:ReadImageOCR>";
        ResponseEntity<byte[]> byteHttpEntity = callUrlWithSessionAndDownloadFile(getStringForSoapCallWithMessage(sMessage), sessionId, new HttpHeaders());
        byte body1[] = byteHttpEntity.getBody();
        HttpHeaders headers = byteHttpEntity.getHeaders();
        List<String> downloadedFileName = headers.get("Content-Disposition");
        logger.info("downloaded filename: "+downloadedFileName.get(0));
        if (body1 == null) {
            return null;
        }
        InputStream myInputStream = new ByteArrayInputStream(body1);

        if (tika == null)
            tika = new Tika();
        return tika.parseToString(myInputStream);
    }

    private String getNextDocumentToOCRResponse(String xDB, String sUser) {
        String nextDocMessage = (CronHelper.createXML("m:CH_GetNextDocToOCR", xDB + sUser));
        HttpEntity<String> response = callUrlWithSession(getStringForSoapCallWithMessage(nextDocMessage), sessionId, new HttpHeaders());
        String body = response.getBody();
        logger.debug("Get Next Doc response: " + body);
        return body;
    }

    private String getStringForSoapCallWithMessage(String message) {
        StringBuffer sb = new StringBuffer();
        sb.append("?Soap_Message=\"");
        sb.append(message);
        sb.append("\"");
        String sendString = SERVER_URL + "/servlets/servlets.CH_CherryServlet" + sb;
        return sendString;
    }
}