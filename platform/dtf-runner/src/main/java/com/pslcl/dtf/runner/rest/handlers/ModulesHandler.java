package com.pslcl.dtf.runner.rest.handlers;

import com.cstkit.common.FutureUtil;
import com.cstkit.common.web.ErrorCodes;
import com.cstkit.common.web.HttpStatus;
import com.cstkit.common.web.VertxUtil;
import com.pslcl.dtf.core.runner.rest.module.Modules;
import com.pslcl.dtf.runner.rest.RestServiceInstance;
import com.pslcl.dtf.runner.rest.storage.NotFoundException;
import com.pslcl.dtf.runner.rest.storage.RestServiceStorage;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModulesHandler implements Handler<RoutingContext>
{
    private final Logger logger;
    private final RestServiceStorage storage;

    public ModulesHandler(RestServiceStorage storage)
    {
        logger = LoggerFactory.getLogger(this.getClass());
        this.storage = storage;
    }

    @Override
    public void handle(RoutingContext context)
    {
        if(context.request().method() != HttpMethod.GET)
        {
            VertxUtil.errorResponse(context, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, "ModulesHandler called with " + context.request().method());
            return;
        }
        String requestPath = context.request().path();
        ModuleEndpoint endpoint = Endpoint.getEndpoint(requestPath);
        switch(endpoint.endpoint)
        {
            case Modules:
                getModules(context);
                break;
            case Module:
                getModule(context, endpoint.moduleId);
                break;
            case Artifacts:
                getArtifactsFromModule(context, endpoint.moduleId);
                break;
            case Report:
                getReportFromModule(context, endpoint.moduleId);
                break;
            case Unknown:
                VertxUtil.errorResponse(context, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, "ModulesHandler called with unknown endpoint format: " + requestPath);
        }
    }

    private void getModules(RoutingContext context)
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

            storage.getModules(filter, order, limit, offset).handle((modules, t) ->
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

    private void getModule(RoutingContext context, String moduleId)
    {
        HttpServerRequest request = context.request();
        try
        {
            storage.getModule(moduleId).handle((module, t) ->
            {
                try
                {
                    if(t != null)
                        throw FutureUtil.unwrap(t);
                    VertxUtil.jsonResponse(context, HttpStatus.OK, module.toJson());
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

    private void getArtifactsFromModule(RoutingContext context, String moduleId)
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

            storage.getArtifactsForModule(moduleId, filter, order, limit, offset).handle((artifacts, t) ->
            {
                try
                {
                    if(t != null)
                        throw FutureUtil.unwrap(t);
                    VertxUtil.jsonResponse(context, HttpStatus.OK, artifacts.toJson());
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

    private void getReportFromModule(RoutingContext context, String moduleId)
    {
        HttpServerRequest request = context.request();
        try
        {
            storage.getReportForModule(moduleId).handle((report, t) ->
            {
                try
                {
                    if(t != null)
                        throw FutureUtil.unwrap(t);
                    VertxUtil.jsonResponse(context, HttpStatus.OK, report.toJson());
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

    private static class ModuleEndpoint
    {
        private String moduleId;
        private Endpoint endpoint;
        private ModuleEndpoint(String moduleId, Endpoint endpoint)
        {
            this.moduleId = moduleId;
            this.endpoint = endpoint;
        }
    }

    private enum Endpoint
    {
        Modules, Module, Artifacts, Report, Unknown;

        private static ModuleEndpoint getEndpoint(String requestPath)
        {
            // /api/v1/modules
            // /api/v1/modules/29
            // /api/v1/modules/29/artifacts
            // /api/v1/modules/29/report
            if(!requestPath.startsWith(RestServiceInstance.ModulesPath))
                return new ModuleEndpoint(null, Unknown);
            int idx = RestServiceInstance.ModulesPath.length();
            if(requestPath.length() == idx)
                return new ModuleEndpoint(null, Modules);
            if(requestPath.charAt(idx) != RestServiceInstance.PathSeparator)
                return new ModuleEndpoint(null, Unknown);
            String remainder = requestPath.substring(++idx);
            idx = remainder.indexOf(RestServiceInstance.PathSeparator);
            if(idx == -1)
                return new ModuleEndpoint(remainder, Module);
            String moduleId = remainder.substring(0, idx);
            remainder = remainder.substring(++idx);
            if(remainder.equals(RestServiceInstance.Artifacts))
                return new ModuleEndpoint(moduleId, Artifacts);
            if(remainder.equals(RestServiceInstance.Report))
                return new ModuleEndpoint(moduleId, Report);
            return new ModuleEndpoint(moduleId, Unknown);
        }
    }
}

