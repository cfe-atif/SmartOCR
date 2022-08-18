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
package com.cfe.smartfolderocr.gson;

import com.cfe.smartfolderocr.cron.CronHelper;
import com.google.gson.annotations.SerializedName;

public class GetNextDocumentDTO {

    @SerializedName("Document")
    public String docNo;
    @SerializedName("Page")
    public String pageNo;
    @SerializedName("Location")
    public String location;
    @SerializedName("MimeType")
    public String mimeType;

    public GetNextDocumentDTO(String xmlToExtract)
    {
        docNo = CronHelper.processResponse(xmlToExtract, "Document");
        pageNo = CronHelper.processResponse(xmlToExtract, "Page");
        location = CronHelper.processResponse(xmlToExtract, "Location");
        mimeType = CronHelper.processResponse(xmlToExtract, "MimeType");
    }

    @Override
    public String toString() {
        return "GetNextDocumentDTO{" +
                "docNo='" + docNo + '\'' +
                ", pageNo='" + pageNo + '\'' +
                ", location='" + location + '\'' +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }
}
