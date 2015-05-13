# Aztec Collect

**Aztec** Barcode **Collect**or for PoC Data Analysis

This Android app is meant to be used to scan 2D barcodes in the Aztec format and collect it's data for further analysis.

The Aztec barcode is currently being used by a Dutch Public Transport company called `NS`, they use KeyCards to open the gates on railway stations.

**Built By BRAINBLENDr**

## Help is needed!

This app can still be perfected and improved in many ways.

Currently there is one fatal issue: The raw bytes from the decoded Aztec code cannot be retrieved using the normal method (.getRawBytes() returns NULL).

(This line here)[https://github.com/zxing/zxing/blob/master/core/src/main/java/com/google/zxing/aztec/decoder/Decoder.java#L79] sets it as null no matter what.

I haven't found a way to circumvent it, if you know how to fix it, please feel free to fork the project and add a PR with the right modifications!

Thank you!

### DISCLAIMER

`Aztec Collect` is provided purely for educational purposes only. 

It is expressly forbidden to use them for any purposes that would violate any domestic or international laws. 

If you do not agree with this policy, please refrain from using this software.
