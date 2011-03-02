#!/bin/bash

cd $1
echo Started $1

for file in `ls .`; do
        /usr/bin/convert $file `echo $file | sed 's/\(.*\)\.\(.*\)/\1/g'`.tif
done
align_image_stack -va AIS_ IMG_????.tif
enfuse -o enfuse.tif AIS_????.tif
/usr/bin/convert -quality 95 enfuse.tif hdr.jpg
rm AIS_????.tif
rm IMG_????.tif
rm enfuse.tif

echo Done $1
