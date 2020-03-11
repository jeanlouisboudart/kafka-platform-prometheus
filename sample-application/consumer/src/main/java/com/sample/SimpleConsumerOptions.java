package com.sample;

import org.apache.commons.cli.*;

public final class SimpleConsumerOptions {

    private static final Options options;

    static {
        options = new Options();
        options.addOption("t", true, "topic name");
        options.addOption("g", false, "check msg keys for gaps");
        options.addOption("v", false, "log batches and individual records");
    };

    private static final CommandLineParser parser = new DefaultParser();

    public final boolean verbose;
    public final boolean checkGaps;
    public final String topicName;

    public SimpleConsumerOptions(String topic, boolean isVerbose, boolean doCheckGaps){
        topicName = topic;
        verbose = isVerbose;
        checkGaps = doCheckGaps;
    }

    public SimpleConsumerOptions(String[] args) throws ParseException {
        CommandLine cmd = parser.parse(options, args);
        topicName = cmd.getOptionValue("t", "workshop_topic_1") + ".*";
        verbose = cmd.hasOption("v");
        checkGaps = cmd.hasOption("g");
        //new SimpleConsumerOptions(cmd.getOptionValue("t", "workshop_topic_1") + ".*", cmd.hasOption("v"), cmd.hasOption("g"));
    }
}