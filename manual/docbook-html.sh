#!/bin/bash
rm src/docbkx/manual/*.html > /dev/null 2>&1
mvn docbkx:generate-html
