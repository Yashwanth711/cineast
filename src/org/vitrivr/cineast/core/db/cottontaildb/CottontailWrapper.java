package org.vitrivr.cineast.core.db.cottontaildb;

import ch.unibas.dmi.dbis.cottontail.grpc.CottonDDLGrpc;
import ch.unibas.dmi.dbis.cottontail.grpc.CottonDDLGrpc.CottonDDLFutureStub;
import ch.unibas.dmi.dbis.cottontail.grpc.CottonDDLGrpc.CottonDDLStub;
import ch.unibas.dmi.dbis.cottontail.grpc.CottonDMLGrpc;
import ch.unibas.dmi.dbis.cottontail.grpc.CottonDMLGrpc.CottonDMLFutureStub;
import ch.unibas.dmi.dbis.cottontail.grpc.CottonDQLGrpc;
import ch.unibas.dmi.dbis.cottontail.grpc.CottonDQLGrpc.CottonDQLBlockingStub;
import ch.unibas.dmi.dbis.cottontail.grpc.CottonDQLGrpc.CottonDQLStub;
import ch.unibas.dmi.dbis.cottontail.grpc.CottontailGrpc.BatchedQueryMessage;
import ch.unibas.dmi.dbis.cottontail.grpc.CottontailGrpc.CreateEntityMessage;
import ch.unibas.dmi.dbis.cottontail.grpc.CottontailGrpc.Entity;
import ch.unibas.dmi.dbis.cottontail.grpc.CottontailGrpc.InsertMessage;
import ch.unibas.dmi.dbis.cottontail.grpc.CottontailGrpc.InsertStatus;
import ch.unibas.dmi.dbis.cottontail.grpc.CottontailGrpc.QueryMessage;
import ch.unibas.dmi.dbis.cottontail.grpc.CottontailGrpc.QueryResponseMessage;
import ch.unibas.dmi.dbis.cottontail.grpc.CottontailGrpc.Schema;
import ch.unibas.dmi.dbis.cottontail.grpc.CottontailGrpc.SuccessStatus;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vitrivr.cineast.core.config.Config;
import org.vitrivr.cineast.core.config.DatabaseConfig;
import org.vitrivr.cineast.core.util.LogHelper;

public class CottontailWrapper implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final InsertStatus INTERRUPTED_INSERT = InsertStatus.newBuilder().setSuccess(false).build();

    private final ManagedChannel channel;
    private final CottonDDLFutureStub definitionFutureStub;
    private final CottonDDLStub definitionStub;
    private final CottonDMLFutureStub managementStub;

    private static final int maxMessageSize = 10_000_000;
    private static final long maxCallTimeOutMs = 300_000; //TODO expose to config

    public CottontailWrapper() {
        DatabaseConfig config = Config.sharedConfig().getDatabase();
        this.channel = NettyChannelBuilder.forAddress(config.getHost(), config.getPort()).usePlaintext(config.getPlaintext()).maxInboundMessageSize(maxMessageSize).build();
        this.definitionFutureStub = CottonDDLGrpc.newFutureStub(channel);
        this.definitionStub = CottonDDLGrpc.newStub(channel);
        this.managementStub = CottonDMLGrpc.newFutureStub(channel);
    }

    public synchronized ListenableFuture<SuccessStatus> createEntity(CreateEntityMessage createMessage) {
        return this.definitionFutureStub.createEntity(createMessage);
    }

    public synchronized void createEntityBlocking(CreateEntityMessage createMessage) {
        ListenableFuture<SuccessStatus> future = this.createEntity(createMessage);
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("error in createEntityBlocking: {}", LogHelper.getStackTrace(e));
        }
    }

    public synchronized ListenableFuture<SuccessStatus> createSchema(String schama) {
        return this.definitionFutureStub.createSchema(CottontailMessageBuilder.schema(schama));
    }

    public synchronized void createSchemaBlocking(String schema) {
        ListenableFuture<SuccessStatus> future = this.createSchema(schema);
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("error in createSchemaBlocking: {}", LogHelper.getStackTrace(e));
        }
    }

    public ListenableFuture<InsertStatus> insert(InsertMessage message) {
        return this.managementStub.insert(message);
    }

    public InsertStatus insertBlocking(InsertMessage message) {
        ListenableFuture<InsertStatus> future = this.insert(message);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("error in insertBlocking: {}", LogHelper.getStackTrace(e));
            return INTERRUPTED_INSERT;
        }
    }

    @Override
    public void close() {
        this.channel.shutdown();
    }

    /**
     * Issues a batched query to the Cottontail DB endpoint.
     *
     * @return The query results (unprocessed).
     */
    public List<QueryResponseMessage> query(QueryMessage query) {
        final ArrayList<QueryResponseMessage> results = new ArrayList<>();
        final CottonDQLBlockingStub stub = CottonDQLGrpc.newBlockingStub(this.channel).withDeadlineAfter(maxCallTimeOutMs, TimeUnit.MILLISECONDS);
        try {
            stub.query(query);
        } catch (StatusRuntimeException e) {
            if (e.getStatus() == Status.DEADLINE_EXCEEDED) {
                LOGGER.error("CottontailWrapper.batchedQuery has timed out (timeout = {}ms).", maxCallTimeOutMs);
            } else {
                LOGGER.error("Error occurred during invocation of CottontailWrapper.batchedQuery:", e.getMessage());
            }
        }
        return results;
    }

    /**
     * Issues a batched query to the Cottontail DB endpoint.
     *
     * @return The query results (unprocessed).
     */
    public List<QueryResponseMessage> batchedQuery(BatchedQueryMessage query) {
        final ArrayList<QueryResponseMessage> results = new ArrayList<>();
        final CottonDQLBlockingStub stub = CottonDQLGrpc.newBlockingStub(this.channel).withDeadlineAfter(maxCallTimeOutMs, TimeUnit.MILLISECONDS);
        try {
            stub.batchedQuery(query).forEachRemaining(results::add);
        } catch (StatusRuntimeException e) {
            if (e.getStatus() == Status.DEADLINE_EXCEEDED) {
                LOGGER.error("CottontailWrapper.batchedQuery has timed out (timeout = {}ms).", maxCallTimeOutMs);
            } else {
                LOGGER.error("Error occurred during invocation of CottontailWrapper.batchedQuery:", e.getMessage());
            }
        }
        return results;
    }

    /**
     * Pings the Cottontail DB endpoint and returns true on success and false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean ping() {
        final CottonDQLBlockingStub stub = CottonDQLGrpc.newBlockingStub(this.channel).withDeadlineAfter(5000, TimeUnit.MILLISECONDS);
        try {
            final SuccessStatus status = stub.ping(Empty.newBuilder().build());
            return true;
        } catch (StatusRuntimeException e) {
            if (e.getStatus() == Status.DEADLINE_EXCEEDED) {
                LOGGER.error("CottontailWrapper.ping has timed out.");
            } else {
                LOGGER.error("Error occurred during invocation of CottontailWrapper.ping:", e.getMessage());

            }
            return false;
        }
    }

    public List<Entity> listEntities(Schema schema) {
        ArrayList<Entity> entities = new ArrayList<>();
        Semaphore semaphore = new Semaphore(1);

        StreamObserver<Entity> observer = new StreamObserver<Entity>() {
            @Override
            public void onNext(Entity value) {
                entities.add(value);
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.error("error in CottonDDL.ListSchemas: {}", LogHelper.getStackTrace(t));
            }

            @Override
            public void onCompleted() {
                semaphore.release();
            }
        };

        try {
            semaphore.acquire();
            definitionStub.listEntities(schema, observer);
            semaphore.tryAcquire(maxCallTimeOutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("Waiting for response in CottontailWrapper.listEntities has been interrupted: {}", LogHelper.getStackTrace(e));
        }

        return entities;
    }
}