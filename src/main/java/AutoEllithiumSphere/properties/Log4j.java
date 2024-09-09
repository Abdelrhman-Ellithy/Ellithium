package AutoEllithiumSphere.properties;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;

@SuppressWarnings("unused")
@Sources({
        "system:properties",
        "file:src/main/resources/properties/log4j2.properties",
        "file:src/main/resources/properties/default/log4j2.properties",
        "classpath:log4j2.properties"
})
public interface Log4j extends FramepropertySetter<Log4j> {

    @Config.Key("property.basePath")
    @Config.DefaultValue("Test-Output/Logs")
    String basePath();

    @Config.Key("appenders")
    @Config.DefaultValue("file, console")
    String appenders();

    // Console Appender Configuration
    @Config.Key("appender.console.type")
    @Config.DefaultValue("Console")
    String appenderConsoleType();

    @Config.Key("appender.console.name")
    @Config.DefaultValue("ConsoleAppender")
    String appenderConsoleName();

    @Config.Key("appender.console.layout.type")
    @Config.DefaultValue("PatternLayout")
    String appenderConsoleLayoutType();

    @Config.Key("appender.console.layout.pattern")
    @Config.DefaultValue("%highlight{[%p]}{FATAL=red blink, ERROR=red bold, WARN=yellow bold, INFO=fg_#0060a8 bold, DEBUG=fg_#43b02a bold, TRACE=black} %style{%m} %style{| @%d{yyyy-MM-dd HH:mm:ss }}{bright_black} %n\"")
    String appenderConsoleLayoutPattern();

    // File Appender Configuration
    @Config.Key("appender.file.type")
    @Config.DefaultValue("File")
    String appenderFileType();

    @Config.Key("appender.file.name")
    @Config.DefaultValue("FileAppender")
    String appenderFileName();

    @Config.Key("appender.file.fileName")
    @Config.DefaultValue("${basePath}/Test.log")
    String appenderFile_FileName();

    @Config.Key("appender.file.append")
    @Config.DefaultValue("true")
    boolean appenderFileAppend();

    @Config.Key("appender.file.layout.type")
    @Config.DefaultValue("PatternLayout")
    String appenderFileLayoutType();

    @Config.Key("appender.file.layout.pattern")
    @Config.DefaultValue("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n")
    String appenderFileLayoutPattern();

    // Root Logger Configuration
    @Config.Key("rootLogger.level")
    @Config.DefaultValue("TRACE")
    String rootLoggerLevel();

    @Config.Key("rootLogger.appenderRefs")
    @Config.DefaultValue("file, console")
    String loggerAppenderRefs();

    @Config.Key("rootLogger.appenderRef.file.ref")
    @Config.DefaultValue("FileAppender")
    String loggerFileAppender();

    @Config.Key("rootLogger.appenderRef.console.ref")
    @Config.DefaultValue("ConsoleAppender")
    String loggerConsoleAppender();
}
