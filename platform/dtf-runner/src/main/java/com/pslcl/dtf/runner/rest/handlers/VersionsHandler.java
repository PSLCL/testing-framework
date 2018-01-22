package com.pslcl.dtf.runner.rest.handlers;

import com.cstkit.common.FutureUtil;
import com.cstkit.common.web.ErrorCodes;
import com.cstkit.common.web.HttpStatus;
import com.cstkit.common.web.TokenInfo;
import com.cstkit.common.web.VertxUtil;
import com.google.gson.Gson;
import com.pslcl.dtf.core.runner.rest.RestVersion;
import com.pslcl.dtf.core.runner.rest.module.Modules;
import com.pslcl.dtf.core.runner.rest.testPlan.TestPlan;
import com.pslcl.dtf.core.storage.DTFStorage;
import com.pslcl.dtf.core.util.executor.BlockingExecutor;
import com.pslcl.dtf.runner.rest.RestServiceInstance;
import com.pslcl.dtf.runner.rest.storage.NotFoundException;
import com.pslcl.dtf.runner.rest.storage.RestServiceStorage;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VersionsHandler implements Handler<RoutingContext>
{
    private final Logger logger;
    private final BlockingExecutor executor;
    private final RestServiceStorage storage;
    private final Gson gson;

    public VersionsHandler(BlockingExecutor executor, RestServiceStorage storage)
    {
        logger = LoggerFactory.getLogger(this.getClass());
        this.storage = storage;
        this.executor = executor;
        gson = new Gson();
    }

    @Override
    public void handle(RoutingContext context)
    {
        if(context.request().method() != HttpMethod.GET)
            VertxUtil.errorResponse(context, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, "VersionsHandler called with " + context.request().method());
        String requestPath = context.request().path();
        get(context);
    }

    private void get(RoutingContext context)
    {
        HttpServerRequest request = context.request();
        try
        {
            String filter = request.getParam(Modules.FilterParam);
            String after = request.getParam(TestPlan.AfterParam);
            storage.getVersions(filter, after).handle((versions, t) ->
            {
                try
                {
                    if(t != null)
                        throw FutureUtil.unwrap(t);
                    VertxUtil.jsonResponse(context, HttpStatus.OK, gson.toJson(versions));
                    //noinspection ReturnOfNull
                    return null;
                }
                catch(NotFoundException ignored)
                {
                    VertxUtil.errorResponse(context, HttpStatus.BAD_REQUEST, ErrorCodes.NOT_FOUND, ignored.getMessage());
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
