package org.klenk.connectivity.iot.dittorrd4j.controller;

import org.klenk.connectivity.iot.dittorrd4j.service.DittoRrd4jService;
import org.rrd4j.core.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;


@RestController
public class DittoRrd4jController {

    public static final int IMAGE_WIDTH = 864;
    public static final int IMAGE_HEIGHT = 480;
    @Autowired
    private DittoRrd4jService service;

    @GetMapping(value="/", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    byte[] getGraph() throws IOException {

        Instant now = Instant.now();
        Instant twentyFourHoursAgo = now.minus(24, ChronoUnit.HOURS);

        BufferedImage bim = service.createBufferedImageOfGraph(
                Util.getTimestamp(new Date(twentyFourHoursAgo.toEpochMilli())),
                Util.getTimestamp(new Date(now.toEpochMilli())),
                IMAGE_WIDTH,
                IMAGE_HEIGHT
        );

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bim, "png", baos);

            baos.flush();
            return baos.toByteArray();
        }
    }
}
