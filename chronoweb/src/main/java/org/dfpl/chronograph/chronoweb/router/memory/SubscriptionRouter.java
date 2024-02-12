package org.dfpl.chronograph.chronoweb.router.memory;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.util.Map.Entry;
import java.util.Set;

import org.dfpl.chronograph.chronoweb.MessageBuilder;
import org.dfpl.chronograph.chronoweb.Server;
import org.dfpl.chronograph.common.KairosProgram;
import org.dfpl.chronograph.common.VertexEvent;
import org.dfpl.chronograph.kairos.AbstractKairosProgram;
import org.dfpl.chronograph.kairos.KairosEngine;
import org.dfpl.chronograph.kairos.gamma.GammaTable;
import org.dfpl.chronograph.kairos.gamma.persistent.LongGammaElement;
import org.dfpl.chronograph.kairos.gamma.persistent.SparseGammaTable;
import org.dfpl.chronograph.kairos.program.IsAfterReachability;
import org.dfpl.chronograph.khronos.memory.manipulation.ChronoVertex;
import org.dfpl.chronograph.khronos.memory.manipulation.ChronoVertexEvent;
import com.tinkerpop.blueprints.Graph;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import static org.dfpl.chronograph.chronoweb.Server.*;

public class SubscriptionRouter extends BaseRouter {

	private KairosEngine kairos;

	public SubscriptionRouter(Graph graph, KairosEngine kairos) {
		super(graph);
		this.kairos = kairos;
	}

	public boolean isAvailableProgram(String kairosProgram) {
		try {
			KairosProgram.valueOf(kairosProgram);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void registerGetSubscriptions(Router router, EventBus eventBus) {
		router.get("/chronoweb/subscribe").handler(routingContext -> {
			JsonArray arr = new JsonArray();
			Set<VertexEvent> set = kairos.getSources();
			for (VertexEvent s : set) {
				arr.add(s.getId());
			}
			sendResult(routingContext, "application/json", arr.toString(), 200);
		});

		Server.logger.info("GET /chronoweb/subscribe router added");
	}

	public void registerSubscribeVertexEventRouter(Router router, EventBus eventBus) {

		router.put("/chronoweb/graph/:time/:kairosProgram/:vertexID").handler(routingContext -> {
			long time;
			try {
				time = Long.parseLong(routingContext.pathParam("time"));
			} catch (Exception e) {
				sendResult(routingContext, "application/json", MessageBuilder.invalidTimeSynta1xException, 400);
				return;
			}

			String kairosProgram = routingContext.pathParam("kairosProgram");
			if (!isAvailableProgram(kairosProgram)) {
				sendResult(routingContext, "application/json", MessageBuilder.noSuchProgramException, 404);
				return;
			}

			String vertexID = routingContext.pathParam("vertexID");
			if (!vPattern.matcher(vertexID).matches()) {
				sendResult(routingContext, "application/json", MessageBuilder.invalidVertexIDException, 400);
				return;
			}

			ChronoVertex v = (ChronoVertex) graph.getVertex(vertexID);
			if (v == null) {
				sendResult(routingContext, "application/json", MessageBuilder.resourceNotFoundException, 404);
				return;
			}

			try {
				if (kairos.getProgram(time, kairosProgram).getGammaTable().getSources().contains(vertexID)) {
					sendResult(routingContext, "application/json", MessageBuilder.sourceAlreadySubscribedException,
							409);
					return;
				}
			} catch (Exception e) {

			}

			ChronoVertexEvent ve = (ChronoVertexEvent) v.getEvent(time);

			if (kairosProgram.equals("IsAfterReachability")) {
				AbstractKairosProgram<?> existing = kairos.getProgram(time, "IsAfterReachability");
				if (existing != null) {
					GammaTable<String, Long> gammaTable = existing.getGammaTable();
					if (gammaTable.getSources().contains(v.getId())) {
						sendResult(routingContext, 406);
						return;
					} else {
						kairos.addSubscription(v, ve.getTime(), new IsAfterReachability(graph, gammaTable));
						sendResult(routingContext, 200);
						return;
					}
				} else {
					String subDirectoryName = Server.baseDirectory + "\\" + ve.getTime() + "_" + kairosProgram;
					File subDirectory = new File(subDirectoryName);
					if (!subDirectory.exists())
						subDirectory.mkdirs();

					SparseGammaTable<String, Long> gammaTable = null;
					try {
						gammaTable = new SparseGammaTable<String, Long>(subDirectoryName, LongGammaElement.class);
					} catch (NotDirectoryException | FileNotFoundException e) {
						sendResult(routingContext, 500);
						return;
					}
					kairos.addSubscription(v, ve.getTime(), new IsAfterReachability(graph, gammaTable));
					sendResult(routingContext, 200);
					return;
				}
			} else {

			}

		});

		Server.logger.info("POST /chronoweb/subscribe/:resource router added");
	}
	
	
	
	

	public void registerGetGammaRouter(Router router, EventBus eventBus) {

		router.get("/chronoweb/gammaTable").handler(routingContext -> {
			JsonArray timeArray = new JsonArray();
			for (Long t : kairos.getTimes()) {
				timeArray.add(t);
			}
			sendResult(routingContext, "application/json", timeArray.toString(), 200);
		});

		router.get("/chronoweb/gammaTable/:time").handler(routingContext -> {
			long time;
			try {
				time = Long.parseLong(routingContext.pathParam("time"));
			} catch (Exception e) {
				sendResult(routingContext, "application/json", MessageBuilder.invalidTimeSynta1xException, 400);
				return;
			}

			JsonArray result = new JsonArray();

			Set<AbstractKairosProgram<?>> programs = kairos.getPrograms(time);
			if (programs == null) {
				sendResult(routingContext, "application/json", MessageBuilder.resourceNotFoundException, 404);
				return;
			}
			for (AbstractKairosProgram<?> program : programs) {
				result.add(program.getName());
			}
			sendResult(routingContext, "application/json", result.toString(), 200);
		});

		router.get("chronoweb/graph/:time/:kairosProgram").handler(routingContext -> {

			long time;
			try {
				time = Long.parseLong(routingContext.pathParam("time"));
			} catch (Exception e) {
				sendResult(routingContext, "application/json", MessageBuilder.invalidTimeSynta1xException, 400);
				return;
			}

			String kairosProgram = routingContext.pathParam("kairosProgram");
			if (!isAvailableProgram(kairosProgram)) {
				sendResult(routingContext, "application/json", MessageBuilder.noSuchProgramException, 404);
				return;
			}
			''
			

			String vertexID = routingContext.pathParam("vertexID");
			if (!vPattern.matcher(vertexID).matches()) {
				
				
				
				sendResult(routingContext, "application/json", MessageBuilder.invalidVertexIDException, 400);
				return;
			}

			ChronoVertex v = (ChronoVertex) graph.getVertex(vertexID);
			if (v == null) {
				sendResult(routingContext, "application/json", MessageBuilder.resourceNotFoundException, 404);
			
				 
				
				
				
				
				  
				return;
			}
			
			JsonObject result = new JsonObject();

			if (vtPattern.matcher(resource).matches()) {
				try {
					String[] arr = resource.split("\\_");
					String vertexID = arr[0];
					long time = Long.parseLong(arr[1]);
					result.put("source", vertexID + "_" + time);
					result.put("recipe", recipeParameter);

					JsonObject gamma = new JsonObject();

					for (Entry<String, Object> entry : kairos.getProgram(time, recipeParameter).getGammaTable()
							.getGamma(vertexID).toMap(true).entrySet()) {
						gamma.put(entry.getKey(), entry.getValue());
					}

					result.put("gamma", gamma);
					sendResult(routingContext, "application/json", result.toString(), 200);
				} catch (Exception e) {
					sendResult(routingContext, 406);
					return;
				}
			} else {
				sendResult(routingContext, 406);
				return;
			}

		});

		Server.logger.info("GET /chronoweb/subscribe/:resource router added");
	}

}
