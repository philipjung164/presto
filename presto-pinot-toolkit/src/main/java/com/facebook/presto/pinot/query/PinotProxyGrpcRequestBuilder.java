/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.pinot.query;

import com.facebook.presto.pinot.PinotScatterGatherQueryClient;
import com.facebook.presto.pinot.grpc.Constants;
import com.facebook.presto.pinot.grpc.GrpcRequestBuilder;
import org.apache.pinot.common.proto.Server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PinotProxyGrpcRequestBuilder
        extends GrpcRequestBuilder
{
    private static final String KEY_OF_PROXY_GRPC_FORWARD_HOST = "FORWARD_HOST";
    private static final String KEY_OF_PROXY_GRPC_FORWARD_PORT = "FORWARD_PORT";

    private String hostName;
    private int port;
    private int requestId;
    private String brokerId = "unknown";
    private boolean enableTrace;
    private boolean enableStreaming;
    private String payloadType;
    private String sql;
    private List<String> segments;
    private Map<String, String> extraMetadata = new HashMap<>();

    public PinotProxyGrpcRequestBuilder setHostName(String hostName)
    {
        this.hostName = hostName;
        return this;
    }

    public PinotProxyGrpcRequestBuilder setPort(int port)
    {
        this.port = port;
        return this;
    }

    public GrpcRequestBuilder setRequestId(int requestId)
    {
        this.requestId = requestId;
        return this;
    }

    public GrpcRequestBuilder setBrokerId(String brokerId)
    {
        this.brokerId = brokerId;
        return this;
    }

    public GrpcRequestBuilder setEnableTrace(boolean enableTrace)
    {
        this.enableTrace = enableTrace;
        return this;
    }

    public GrpcRequestBuilder setEnableStreaming(boolean enableStreaming)
    {
        this.enableStreaming = enableStreaming;
        return this;
    }

    public GrpcRequestBuilder setSql(String sql)
    {
        payloadType = Constants.Request.PayloadType.SQL;
        this.sql = sql;
        return this;
    }

    public GrpcRequestBuilder addExtraMetadata(Map<String, String> extraMetadata)
    {
        this.extraMetadata.putAll(extraMetadata);
        return this;
    }

    public GrpcRequestBuilder setSegments(List<String> segments)
    {
        this.segments = segments;
        return this;
    }

    public Server.ServerRequest build()
    {
        if (payloadType == null || segments.isEmpty()) {
            throw new PinotScatterGatherQueryClient.PinotException(PinotScatterGatherQueryClient.ErrorCode.PINOT_UNCLASSIFIED_ERROR, "Query and segmentsToQuery must be set");
        }
        if (!payloadType.equals(Constants.Request.PayloadType.SQL)) {
            throw new RuntimeException("Only [SQL] Payload type is allowed: " + payloadType);
        }
        Map<String, String> metadata = new HashMap<>();
        metadata.put(Constants.Request.MetadataKeys.REQUEST_ID, Integer.toString(requestId));
        metadata.put(Constants.Request.MetadataKeys.BROKER_ID, brokerId);
        metadata.put(Constants.Request.MetadataKeys.ENABLE_TRACE, Boolean.toString(enableTrace));
        metadata.put(Constants.Request.MetadataKeys.ENABLE_STREAMING, Boolean.toString(enableStreaming));
        metadata.put(Constants.Request.MetadataKeys.PAYLOAD_TYPE, payloadType);
        metadata.put(KEY_OF_PROXY_GRPC_FORWARD_HOST, this.hostName);
        metadata.put(KEY_OF_PROXY_GRPC_FORWARD_PORT, String.valueOf(this.port));
        extraMetadata.forEach((k, v) -> metadata.put(k, v));
        return Server.ServerRequest.newBuilder()
            .putAllMetadata(metadata)
            .setSql(sql)
            .addAllSegments(segments)
            .build();
    }
}
