package com.yuhe.american.spout;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;

import org.apache.log4j.Logger;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import com.yuhe.american.log_modules.AbstractLogModule;
import com.yuhe.american.log_modules.LogIndexes;


public class RedisSpout extends BaseRichSpout {
	private static final long serialVersionUID = 1L;
	private SpoutOutputCollector collector;
	public static Logger logger = Logger.getLogger(RedisSpout.class);
	//线上环境用这个，切记
//	private static Jedis jedis = new Jedis("127.0.0.1", 16379){ 
//		{
//			auth("FfsOI89KL");
//		}
//	};
	//测试环境用这个
	private static Jedis jedis = new Jedis("192.168.1.98", 6379);
	
	// lua脚本，用于在redis中批量获取队列内容
	private String LUA_SCRIPT = "local Result = {} local Length = redis.call('LLEN',KEYS[1]) "
			+ "for Index = 0, Length-1 do local Value = redis.call('LPOP',KEYS[1]) if Value then "
			+ "table.insert(Result, Value) end end return Result";

	@SuppressWarnings("rawtypes")
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		this.collector = collector;
	}

	@SuppressWarnings("unchecked")
	public void nextTuple() {
		// get data from redis
		Map<String, AbstractLogModule> indexMap = LogIndexes.GetIndexMap();
		Iterator<String> it = indexMap.keySet().iterator();
		while (it.hasNext()) {
			String staticsIndex = (String) it.next();
			List<String> logList = (List<String>) jedis.eval(LUA_SCRIPT, 1, staticsIndex);
			if (logList.size() > 0)
				collector.emit(new Values(staticsIndex, logList));
		}

	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("staticsIndex", "logList"));
	}

}
