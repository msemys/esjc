# EventStore Java Client


Download the latest version of EventStore message formats (requires maven >= 3.3.1):

    mvn antrun:run@download-proto

Download 'Protocol Buffers' compiler:

    https://developers.google.com/protocol-buffers/docs/downloads
        or
    sudo apt-get install protobuf-compiler

Compile proto file:

    mvn generate-sources
