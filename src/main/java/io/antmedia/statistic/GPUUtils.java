package io.antmedia.statistic;

import static org.bytedeco.javacpp.nvml.NVML_SUCCESS;
import static org.bytedeco.javacpp.nvml.nvmlDeviceGetCount_v2;
import static org.bytedeco.javacpp.nvml.nvmlDeviceGetHandleByIndex_v2;
import static org.bytedeco.javacpp.nvml.*;
import static org.bytedeco.javacpp.nvml.nvmlInit_v2;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.nvml;
import org.bytedeco.javacpp.nvml.nvmlDevice_st;
import org.bytedeco.javacpp.nvml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPUUtils {

	private static Logger logger = LoggerFactory.getLogger(GPUUtils.class);

	private static GPUUtils instance;

	private static boolean noGPU = true;

	private Integer deviceCount = null;

	public static class MemoryStatus {
		private long memoryTotal;
		private long memoryUsed;
		private long memoryFree;

		public MemoryStatus(long memoryTotal, long memoryUsed, long memoryFree) {
			this.memoryTotal = memoryTotal;
			this.memoryUsed = memoryUsed;
			this.memoryFree = memoryFree;
		}

		public long getMemoryTotal() {
			return memoryTotal;
		}

		public long getMemoryUsed() {
			return memoryUsed;
		}

		public long getMemoryFree() {
			return memoryFree;
		}
	}

	private GPUUtils() {}

	public static GPUUtils getInstance() {
		if(instance == null) {
			instance = new GPUUtils();

			try {
				Class.forName("org.bytedeco.javacpp.nvml");

				Loader.load(nvml.class);
				int result = nvmlInit_v2();
				if (result == NVML_SUCCESS) {
					logger.info("cuda initialized {}", "");
					noGPU = false;
				}
			}
			catch (UnsatisfiedLinkError e) {
				logger.info("no cuda installed {}", "");
			} 
			catch (ClassNotFoundException e) {
				logger.info("nvml class not found {}", "");
			}
		}
		return instance;
	}

	public int getDeviceCount() {
		if(noGPU) {
			return 0;
		}

		if(deviceCount == null) {
			IntPointer count = new IntPointer(1);
			int result = nvmlDeviceGetCount_v2(count);
			if (result == NVML_SUCCESS) {
				deviceCount = count.get();
			}
			else {
				deviceCount = 0;
			}
		}
		return deviceCount;
	}

	public nvmlDevice_st getDevice(int deviceIndex) 
	{
		if (!noGPU) {
			nvmlDevice_st device = new nvmlDevice_st();
			if (nvmlDeviceGetHandleByIndex_v2(deviceIndex, device) == NVML_SUCCESS) {
				return device;
			}
		}
		return null;
	}

	private nvmlUtilization_t getUtilization(int deviceNo) {
		nvmlDevice_st device = null;
		if ((device = getDevice(deviceNo)) != null) {
			nvmlUtilization_t deviceUtilization = new nvmlUtilization_t();
			if (nvmlDeviceGetUtilizationRates(device, deviceUtilization) == NVML_SUCCESS) {
				return deviceUtilization;
			}
		}
		return null;
	} 


	public MemoryStatus getMemoryStatus(int deviceNo) {
		nvmlDevice_st device = null;
		if ((device = getDevice(deviceNo)) != null) {
			nvmlMemory_t nvmlMemory = new nvmlMemory_t();
			if (nvmlDeviceGetMemoryInfo(device, nvmlMemory) == NVML_SUCCESS) {
				return new MemoryStatus(nvmlMemory.total(), nvmlMemory.used(), nvmlMemory.free());
			}
		}
		return null;
	}
	
	public String getDeviceName(int deviceIndex) {
		nvmlDevice_st device = null;
		if ((device = getDevice(deviceIndex)) != null) {
			byte[] name = new byte[64];
			if (nvmlDeviceGetName(device, name, name.length) == NVML_SUCCESS) {
				return new String(name, 0, name.length);
			}
		}
		return null;
	}
	
	public int getMemoryUtilization(int deviceNo) {
		nvmlUtilization_t utilization = getUtilization(deviceNo);
		if(utilization != null) {
			return utilization.memory();
		}
		return -1;
	}

	public int getGPUUtilization(int deviceNo) {
		nvmlUtilization_t utilization = getUtilization(deviceNo);
		if(utilization != null) {
			return utilization.gpu();
		}
		return -1;
	}
}