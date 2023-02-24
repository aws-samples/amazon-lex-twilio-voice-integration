FROM tomcat:8-jdk11-corretto

COPY war-output/TwilioWaitAndContinue-1.0.war /usr/local/tomcat/webapps/

COPY configurations/tomcat-configuration/tomcat-users.xml $CATALINA_HOME/conf/tomcat-users.xml
COPY configurations/tomcat-configuration/context.xml $CATALINA_HOME/webapps/manager/META-INF/context.xml

#ENV JAVA_OPTS="-Dlog4j.debug"

EXPOSE 8080

CMD ["catalina.sh", "run"]
