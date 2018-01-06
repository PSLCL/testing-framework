package com.pslcl.dtf.runner.rest.handlers;

import com.cstkit.common.FutureUtil;
import com.cstkit.common.web.ErrorCodes;
import com.cstkit.common.web.HttpStatus;
import com.cstkit.common.web.VertxUtil;
import com.pslcl.dtf.core.runner.rest.runRates.RunRates;
import com.pslcl.dtf.core.util.executor.BlockingExecutor;
import com.pslcl.dtf.runner.rest.storage.RestServiceStorage;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RunRatesHandler implements Handler<RoutingContext>
{
    private final Logger logger;
    private final BlockingExecutor executor;
    private final RestServiceStorage storage;

    public RunRatesHandler(BlockingExecutor executor, RestServiceStorage storage)
    {
        logger = LoggerFactory.getLogger(this.getClass());
        this.storage = storage;
        this.executor = executor;
    }

    @Override
    public void handle(RoutingContext context)
    {
        if(context.request().method() != HttpMethod.GET)
            VertxUtil.errorResponse(context, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, "RunRatesHandler called with " + context.request().method());
        String requestPath = context.request().path();
        get(context);
    }

    private void get(RoutingContext context)
    {
        HttpServerRequest request = context.request();
        try
        {
            String fromStr = request.getParam(RunRates.FromParam);
            String toStr = request.getParam(RunRates.ToParam);
            String bucketStr = request.getParam(RunRates.BucketParam);
            Long from = null;
            Long to = null;
            Long bucket = null;
            if(fromStr != null)
            {
                try
                {
                    from = Long.parseLong(fromStr);
                }catch(Throwable ignored)
                {
                    VertxUtil.errorResponse(context, HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST_PARAMETER, "Invalid from parameter");
                    return;
                }
            }
            if(toStr != null)
            {
                try
                {
                    to = Long.parseLong(toStr);
                }catch(Throwable ignored)
                {
                    VertxUtil.errorResponse(context, HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST_PARAMETER, "Invalid to parameter");
                    return;
                }
            }

            if(bucketStr != null)
            {
                try
                {
                    bucket = Long.parseLong(bucketStr);
                }catch(Throwable ignored)
                {
                    VertxUtil.errorResponse(context, HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST_PARAMETER, "Invalid bucket parameter");
                    return;
                }
            }

            storage.getRunRates(from, to, bucket).handle((runRates, t) ->
            {
                try
                {
                    if(t != null)
                        throw FutureUtil.unwrap(t);
                    VertxUtil.jsonResponse(context, HttpStatus.OK, runRates.toJson());
                    //noinspection ReturnOfNull
                    return null;
                }
                catch(Throwable throwable)
                {
                    logger.warn("Internal error", throwable);
                    VertxUtil.errorResponse(context, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, "An internal storage error has occured");
                    //noinspection ReturnOfNull
                    return null;
                }
            });
        }catch(Throwable t)
        {
            logger.warn("Internal error", t);
            VertxUtil.errorResponse(context, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, "An internal handler error has occured");
        }
    }
}

