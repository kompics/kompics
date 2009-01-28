#!/bin/bash
rm src/docbkx/manual/*.pdf > /dev/null 2>&1
mvn docbkx:generate-pdf
