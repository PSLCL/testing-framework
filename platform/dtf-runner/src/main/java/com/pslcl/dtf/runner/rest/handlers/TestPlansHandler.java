package com.pslcl.dtf.runner.rest.handlers;

import com.cstkit.common.FutureUtil;
import com.cstkit.common.web.ErrorCodes;
import com.cstkit.common.web.HttpStatus;
import com.cstkit.common.web.VertxUtil;
import com.google.gson.Gson;
import com.pslcl.dtf.core.runner.rest.module.Modules;
import com.pslcl.dtf.core.runner.rest.testPlan.TestPlan;
import com.pslcl.dtf.runner.rest.RestServiceInstance;
import com.pslcl.dtf.runner.rest.storage.NotFoundException;
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
    private final Gson gson;

    public TestPlansHandler(RestServiceStorage storage)
    {
        logger = LoggerFactory.getLogger(this.getClass());
        this.storage = storage;
        gson = new Gson();
    }

    @Override
    public void handle(RoutingContext context)
    {
        if(context.request().method() != HttpMethod.GET)
        {
            VertxUtil.errorResponse(context, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, "TestPlansHandler called with " + context.request().method());
            return;
        }
        String requestPath = context.request().path();
        PlanEndpoint endpoint = Endpoint.getEndpoint(requestPath);
        switch(endpoint.endpoint)
        {
            case Plans:
                getPlans(context);
                break;
            case Plan:
                getPlan(context, endpoint.planId);
                break;
            case Tests:
                getTests(context, endpoint.planId);
                break;
            case Test:
                getTest(context, endpoint.planId, endpoint.testId);
                break;
            case Unknown:
                VertxUtil.errorResponse(context, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, "TestPlansHandler called with unknown endpoint format: " + requestPath);
        }
    }

    private void getPlans(RoutingContext context)
    {
        HttpServerRequest request = context.request();
        try
        {
            String filter = request.getParam(Modules.FilterParam);
            String order = request.getParam(Modules.OrderParam);
            String limit = request.getParam(Modules.LimitParam);
            String offsetStr = request.getParam(Modules.OffsetParam);

            Integer offset = null;
            if(offsetStr != null)
            {
                try
                {
                    offset = Integer.parseInt(offsetStr);
                }catch(Throwable ignored)
                {
                    VertxUtil.errorResponse(context, HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST_PARAMETER, "Invalid offset parameter");
                    return;
                }
            }

            storage.getTestPlans(filter, order, limit, offset).handle((modules, t) ->
            {
                try
                {
                    if(t != null)
                        throw FutureUtil.unwrap(t);
                    VertxUtil.jsonResponse(context, HttpStatus.OK, modules.toJson());
                    //noinspection ReturnOfNull
                    return null;
                }
                catch(IllegalArgumentException ignored)
                {
                    VertxUtil.errorResponse(context, HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST_PARAMETER, ignored.getMessage());
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

    private void getPlan(RoutingContext context, String planId)
    {
        HttpServerRequest request = context.request();
        try
        {
            String filter = request.getParam(Modules.FilterParam);
            String after = request.getParam(TestPlan.AfterParam);
            storage.getTestPlan(planId, filter, after).handle((plan, t) ->
            {
                try
                {
                    if(t != null)
                        throw FutureUtil.unwrap(t);
                    VertxUtil.jsonResponse(context, HttpStatus.OK, plan.toJson());
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

    private void getTests(RoutingContext context, String planId)
    {
        try
        {
            storage.getTestsForTestPlan(planId).handle((tests, t) ->
            {
                try
                {
                    if(t != null)
                        throw FutureUtil.unwrap(t);
                    VertxUtil.jsonResponse(context, HttpStatus.OK, gson.toJson(tests));
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

    private void getTest(RoutingContext context, String planId, String testId)
    {
        try
        {
            storage.getTestForTestPlan(planId, testId).handle((test, t) ->
            {
                try
                {
                    if(t != null)
                        throw FutureUtil.unwrap(t);
                    VertxUtil.jsonResponse(context, HttpStatus.OK, gson.toJson(test));
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

    private static class PlanEndpoint
    {
        private String planId;
        private String testId;
        private Endpoint endpoint;

        private PlanEndpoint(String planId, Endpoint endpoint)
        {
            this.planId = planId;
            testId = null;
            this.endpoint = endpoint;
        }

        private PlanEndpoint(String planId, String testId, Endpoint endpoint)
        {
            this.planId = planId;
            this.testId = testId;
            this.endpoint = endpoint;
        }
    }

    private enum Endpoint
    {
        Plans, Plan, Tests, Test, Unknown;

        private static PlanEndpoint getEndpoint(String requestPath)
        {
            // /api/v1/test_plans
            // /api/v1/test_plans/11
            // /api/v1/test_plans/11/tests
            // /api/v1/test_plans/11/tests/:id
            if(!requestPath.startsWith(RestServiceInstance.TestPlansPath))
                return new PlanEndpoint(null, Unknown);
            int idx = RestServiceInstance.TestPlansPath.length();
            if(requestPath.length() == idx)
                return new PlanEndpoint(null, Plans);
            if(requestPath.charAt(idx) != RestServiceInstance.PathSeparator)
                return new PlanEndpoint(null, Unknown);
            String remainder = requestPath.substring(++idx);
            idx = remainder.indexOf(RestServiceInstance.PathSeparator);
            if(idx == -1)
                return new PlanEndpoint(remainder, Plan);
            String planId = remainder.substring(0, idx);
            remainder = remainder.substring(++idx);
            if(remainder.equals(RestServiceInstance.Tests))
                return new PlanEndpoint(planId, Tests);
            idx = remainder.indexOf(RestServiceInstance.PathSeparator);
            if(idx == -1)
                return new PlanEndpoint(planId, Unknown);
            remainder = remainder.substring(++idx);
            return new PlanEndpoint(planId, remainder, Test);
        }
    }
}

