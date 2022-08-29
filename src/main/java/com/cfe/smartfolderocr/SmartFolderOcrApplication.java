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
package com.cfe.smartfolderocr;

import ch.qos.logback.classic.spi.Configurator;
import com.cfe.smartfolderocr.cron.SmartFolderOCRCronJob;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SmartFolderOcrApplication extends SpringBootServletInitializer {

    private static SmartFolderOCRCronJob cronJob;

    @Autowired
    public void setCronJob(SmartFolderOCRCronJob cronJob){
        this.cronJob = cronJob;
    }

    public static void main(String[] args) {
        SpringApplication.run(SmartFolderOcrApplication.class, args);
        cronJob.runCronJob();
    }
}
