package org.klenk.connectivity.iot.dittorrd4j.service;

import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class DittoRrd4jService {


    public static final String RRDPATH = "./test.rrd";
    public static final String DS_TOTAL_BYTES_SENT = "totalBytesSent";
    public static final String DS_TOTAL_BYTES_RECEIVED = "totalBytesReceived";

    public DittoRrd4jService() throws FileNotFoundException {
    }

    public void createRrdDb() throws IOException {

        Instant now = Instant.now();
        Instant twentyFourHoursAgo = now.minus(24, ChronoUnit.HOURS);

        RrdDef rrdDef = new RrdDef(RRDPATH);
        rrdDef.setStartTime(Util.getTimestamp(new Date(twentyFourHoursAgo.toEpochMilli())) - 1);
        rrdDef.addDatasource(DS_TOTAL_BYTES_SENT, DsType.COUNTER, 600, Double.NaN, Double.NaN);
        rrdDef.addDatasource(DS_TOTAL_BYTES_RECEIVED, DsType.COUNTER, 600, Double.NaN, Double.NaN);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 12 * 24); // 5 minutes * 12 * 24 = 1 day
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 12, 24 * 14); // 1 hour * 24 * 14 = 14 days
        try (RrdDb rrdDb = RrdDb.getBuilder().setRrdDef(rrdDef).build()) {
        }
    }

    public void addSample(long timeInSecondsSinceEpoch, double totalBytesSent, double totalBytesReceived) throws IOException{

        try (RrdDb rrdDb = RrdDb.getBuilder().setPath(RRDPATH).build()) {
            Sample sample = rrdDb.createSample();

            String sampleLine = new StringBuilder()
                    .append(timeInSecondsSinceEpoch)
                    .append(":")
                    .append(totalBytesSent)
                    .append(":")
                    .append(totalBytesReceived)
                    .toString();

            System.out.println(sampleLine);

            sample.setAndUpdate(sampleLine);
        }
    }

    public BufferedImage createBufferedImageOfGraph(long startTimeSeconds, long endTimeSeconds,
                                                    int imageWidth, int imageHeight) throws IOException {

        RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.setTimeSpan(startTimeSeconds, endTimeSeconds);

        graphDef.setTitle("Bytes sent/received as reported by communication module");
        graphDef.setVerticalLabel("Bytes sent/received per second");

        graphDef.setWidth(imageWidth);
        graphDef.setHeight(imageHeight);

        graphDef.datasource(DS_TOTAL_BYTES_SENT, RRDPATH, DS_TOTAL_BYTES_SENT, ConsolFun.AVERAGE);
        graphDef.datasource(DS_TOTAL_BYTES_RECEIVED, RRDPATH, DS_TOTAL_BYTES_RECEIVED, ConsolFun.AVERAGE);
        graphDef.stack(DS_TOTAL_BYTES_SENT, new Color(0, 0xFF, 0), "Bytes sent");
        graphDef.stack(DS_TOTAL_BYTES_RECEIVED, new Color(0, 0, 0xFF), "Bytes received");

        graphDef.comment("\\r");

        RrdGraph graph = new RrdGraph(graphDef);
        BufferedImage bufferedImage = new BufferedImage(
                graph.getRrdGraphInfo().getWidth(),
                graph.getRrdGraphInfo().getHeight(),
                BufferedImage.TYPE_INT_RGB);
        graph.render(bufferedImage.getGraphics());

        return bufferedImage;
    }
}
