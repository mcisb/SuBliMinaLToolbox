# SuBliMinaLToolbox

Installation
------------

1. Download Marvin Suite from Chemaxon: https://www.chemaxon.com/download/.

2. Type the following commands to install Marvin Suite jars in your local Maven repository.
Note that the -Dfile path paramater will have to be changed for Windows / Linux installations to match the ChemAxon installation location.

`mvn install:install-file -Dfile=MarvinBeans.jar -DgroupId=chemaxon -DartifactId=MarvinBeans -Dversion=6.0.2 -Dpackaging=jar`