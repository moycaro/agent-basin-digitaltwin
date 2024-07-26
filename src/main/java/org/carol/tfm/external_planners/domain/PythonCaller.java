package org.carol.tfm.external_planners.domain;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

public class PythonCaller {
    private final Log log = LogFactory.getLog(this.getClass());

    public Optional<String> callAndWait(String filePath) {
        try {
            String line = "/home/carol/anaconda3/bin/python3 " + filePath;
            CommandLine cmdLine = CommandLine.parse(line);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);

            DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(streamHandler);

            int exitCode = executor.execute(cmdLine);
            final String outputText = outputStream.toString();
            log.info( "Exited with code: " + exitCode );

            return Optional.of( outputText );
        } catch (Exception ex) {
            log.error( "Can not execute python script.", ex);
        }

        return Optional.empty();
    }
}
