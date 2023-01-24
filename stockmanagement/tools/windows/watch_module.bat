mvn org.openmrs.maven.plugins:openmrs-sdk-maven-plugin:setup-sdk
REM I linked my f:\workspace settings file to c:\users\jssenya
mvn openmrs-sdk:setup-sdk -U
6 -Ddistro=org.openmrs.module:ugandaemr:LATEST-SNAPSHOT
mvn openmrs-sdk:run -DserverId=openmrs

REM in module folder
mvn openmrs-sdk:watch -DserverId=openmrs


REM  

jdbc:mysql://127.0.0.1:3306/openmrs
jdbc:mysql://127.0.0.1:3306/kemr
jdbc:mysql://127.0.0.1:3306/ugandaemr?autoReconnect\=true&useUnicode\=true&characterEncoding\=UTF-8&sessionVariables\=default_storage_engine%3DInnoDB

REM made links from  users folder to f:\ for the following
REM mklink /D C:\Users\jssenyan\openmrs F:\workspace\openmrs
REM mklink F:\workspace\settings.xml C:\Users\jssenyan\.m2\settings.xml 


REM cd into openmrs module ugandaemr folder after cloning it and run below
REM mvn openmrs-sdk:watch -DserverId=openmrs

REM Error: Module UgandaEMR cannot be started because it requires the following module(s): patientflags Please install and start these modules first.



mvn  openmrs-sdk:run -DserverId=kemr

org.openmrs.distro:referenceapplication-package:2.3.1


mvn openmrs-sdk:setup -Ddistro=org.openmrs.module:ugandaemr:LATEST-SNAPSHOT
mvn openmrs-sdk:setup -Ddistro=org.openmrs.module:ugandaemr:3.3.8
mvn openmrs-sdk:run -DserverId=ugandaemr -DskipTests
mvn openmrs-sdk:watch -DserverId=ugandaemr -DskipTests
mvn openmrs-sdk:unwatch -DserverId=ugandaemr -DskipTests