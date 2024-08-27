package com.kunzhut.httpserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {

    private static final String defaultConfig = "Config: {\n" +
            "\n" +
            "Header: {\n" +
            "HTTP/2.0 200\n" +
            "Server: Test/1.0\n" +
            "}\n" +
            "\n" +
            "Root: {\n" +
            "\n" +
            "Path: /\n" +
            "\n" +
            "}\n" +
            "\n" +
            "}";
    private static final int HeaderGroup = 1;
    private static final int RootGroup = 2;
    private static final Pattern configPattern = Pattern.compile("(?<!.)Config:\\s*\\{\\n[\\n\\s]*(?:(?:Header:\\s*\\{\\n([^}]{1,}\\n)}[\\n\\s]{1,}){0,1}|(?:Root:\\s*\\{\\n{1,}\\s*Path:\\s*([^\\n]{1,})[\\n\\s]{1,}}[\\n\\s]{1,}){0,1}){2}}(?!.)",
           Pattern.MULTILINE | Pattern.DOTALL);

    private final MatchResult configMatchResult;

    public Config(Path path) throws IOException,ConfigValidationException {
       configMatchResult = getMatchResultFromPath(path);
    }


    public Config(String config) throws ConfigValidationException {
        configMatchResult = getMatchResultFromString(config);
    }

    private MatchResult getMatchResultFromPath(Path path) throws IOException, ConfigValidationException {
        return checkConfig(configPattern.matcher(new String(Files.readAllBytes(path))));

    }

    private MatchResult getMatchResultFromString(String config) throws ConfigValidationException {
        return checkConfig(configPattern.matcher(config));
    }

    private MatchResult checkConfig(Matcher m) throws ConfigValidationException{
        if (!m.find()) throw new ConfigValidationException("Config is incorrect");
        return m.toMatchResult();
    }

    public static Config getDefault() {
        try {
            return new Config(defaultConfig);
        } catch (Exception ignored) {
            return null;
        }
    }

    protected String getConfig(){
        return configMatchResult.group();
    }
    protected Header getHeader(){
       return new Header();
    }
    protected String getRoot(){
        return configMatchResult.group(RootGroup);
    }

    protected class Header{
        private final StringBuilder Header = new StringBuilder(configMatchResult.group(Config.HeaderGroup));
        private Header() {

        }
        @Override
        public String toString() {
            return this.Header.toString();
        }
        protected synchronized void changeParameter(String name, String newValue){
            changeParameter(Header, name,newValue);
        }
        protected String getParameterValue(String name){
            return getParameterValue(Header,name);
        }

        private void changeParameter(StringBuilder data, String name, String newValue){
            Matcher ParameterChanger = getParameterPattern(name).matcher(data);
            if (ParameterChanger.find()){
                data.replace(ParameterChanger.start(), ParameterChanger.end(),newValue);
            }
        }

        protected synchronized void addBlankLine(){
            Header.append("\n");
        }

        private String getParameterValue(StringBuilder data, String name){
            Matcher m = getParameterPattern(name).matcher(data);
            return m.find() ? m.group() : null;
        }

        private Pattern getParameterPattern(String ParName){
                 return Pattern.compile("(?<=" + ParName +":)[^\\n]{1,}");
        }

    }
}
