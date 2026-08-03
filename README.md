# Order Book Engine
A high performance, low latency **Java Order Book** built using the LMAX Disruptor pattern for high-throughput. Uses Lock-free concurrency and object pooling to minimize GC pauses.

Orders are received asynchronously through Netty (see client for example implementation) before being routed to a shard for single-threaded matching.
