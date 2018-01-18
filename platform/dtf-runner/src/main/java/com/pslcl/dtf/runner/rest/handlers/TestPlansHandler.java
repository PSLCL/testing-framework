package com.pslcl.dtf.runner.rest.handlers;

import com.cstkit.common.web.ErrorCodes;
import com.cstkit.common.web.HttpStatus;
import com.cstkit.common.web.TokenInfo;
import com.cstkit.common.web.VertxUtil;
import com.pslcl.dtf.core.runner.rest.RestVersion;
import com.pslcl.dtf.core.storage.DTFStorage;
import com.pslcl.dtf.core.util.executor.BlockingExecutor;
import com.pslcl.dtf.runner.rest.RestServiceInstance;
import com.pslcl.dtf.runner.rest.storage.RestServiceStorage;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestPlansHandler implements Handler<RoutingContext>
{
    private final Logger logger;
    private final RestServiceStorage storage;

    public TestPlansHandler(RestServiceStorage storage)
    {
        logger = LoggerFactory.getLogger(this.getClass());
        this.storage = storage;
    }

    @Override
    public void handle(RoutingContext context)
    {
        if(context.request().method() != HttpMethod.GET)
            VertxUtil.errorResponse(context, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, "TestPlansHandler called with " + context.request().method());
        String requestPath = context.request().path();
        get(context);
    }

    private void get(RoutingContext context)
    {
        TokenInfo tokenInfo = context.get(VertxUtil.TOKEN_INFO_KEY);
        HttpServerRequest request = context.request();
        try
        {
//            String pagingToken = request.getParam(Page.PagingToken);
//            int count;
//            try
//            {
//                count = Page.normalizeNumberOfItems(request.getParam(Page.PagingCount));
//            }catch(Throwable ignored)
//            {
//                VertxUtil.errorResponse(context, HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST_PARAMETER, "Invalid paging count");
//                return;
//            }
//
//            storage.getKeyspaces(tokenInfo.getDomain(), pagingToken, count).handle((keyspacePage, t) ->
//            {
//                try
//                {
//                    if(t != null)
//                        throw FutureUtil.unwrap(t);
//                    VertxUtil.jsonResponse(context, HttpStatus.OK, keyspacePage.toJson());
//                    //noinspection ReturnOfNull
//                    return null;
//                }
//                catch(IllegalArgumentException ignored)
//                {
//                    VertxUtil.errorResponse(context, HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST_PARAMETER, "Invalid paging token");
//                    //noinspection ReturnOfNull
//                    return null;
//                }
//                catch(Throwable throwable)
//                {
//                    logger.warn("Internal error", throwable);
//                    VertxUtil.errorResponse(context, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, "An internal storage error has occured");
//                    //noinspection ReturnOfNull
//                    return null;
//                }
//            });
        }catch(Throwable t)
        {
            logger.warn("Internal error", t);
            VertxUtil.errorResponse(context, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, "An internal handler error has occured");
        }
    }
}

