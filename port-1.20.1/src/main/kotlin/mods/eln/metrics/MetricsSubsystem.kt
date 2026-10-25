package mods.eln.metrics

import mods.eln.Eln
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private data class SimMnaMetricsPayload(
    val subSystemCount: Int,
    val inversionCount: Int,
    val singularMatrixCount: Int,
    val inversionAverageNanoseconds: Double,
    val inversionMaximumNanoseconds: Long
)

private data class SimRuntimeMetricsPayload(
    val avgTickMicroseconds: Double,
    val electricalMicroseconds: Double,
    val thermalFastMicroseconds: Double,
    val thermalSlowMicroseconds: Double,
    val slowMicroseconds: Double,
    val subSystemCount: Int,
    val electricalProcessCount: Int,
    val thermalFastLoadCount: Int,
    val thermalFastConnectionCount: Int,
    val thermalFastProcessCount: Int,
    val thermalSlowLoadCount: Int,
    val thermalSlowConnectionCount: Int,
    val thermalSlowProcessCount: Int,
    val slowProcessCount: Int
)

private sealed interface MetricsPayload
private data class MnaPayload(val payload: SimMnaMetricsPayload) : MetricsPayload
private data class RuntimePayload(val payload: SimRuntimeMetricsPayload) : MetricsPayload

private interface MetricsSink {
    fun publishMna(payload: SimMnaMetricsPayload)
    fun publishRuntime(payload: SimRuntimeMetricsPayload)
}

object MetricsSubsystem {
    private const val DUMMY_SERVER_NAME = "dummy"
    private val running = AtomicBoolean(false)
    private val simulatorMetricsActive = AtomicBoolean(false)
    private val queue = ConcurrentLinkedQueue<MetricsPayload>()
    private val sinks = mutableListOf<MetricsSink>()
    private val dummyPublishCount = AtomicInteger(0)
    private val dummyInversionCount = AtomicInteger(0)
    private val dummySingularCount = AtomicInteger(0)

    @Volatile
    private var worker: Thread? = null

    @JvmStatic
    @Synchronized
    fun refreshFromConfig() {
        simulatorMetricsActive.set(false)
        sinks.clear()
        dummyPublishCount.set(0)
        dummyInversionCount.set(0)
        dummySingularCount.set(0)
        val serverName = Eln.simMetricsMqttServer
        val streamId = Eln.simMetricsId.ifBlank { "server" }
        if (!Eln.mqttEnabled || !Eln.simMetricsEnabled) {
            stopWorker()
            return
        }
        if (serverName.equals(DUMMY_SERVER_NAME, ignoreCase = true)) {
            sinks.add(
                object : MetricsSink {
                    override fun publishMna(payload: SimMnaMetricsPayload) {
                        dummyPublishCount.incrementAndGet()
                        dummyInversionCount.addAndGet(payload.inversionCount)
                        dummySingularCount.addAndGet(payload.singularMatrixCount)
                    }

                    override fun publishRuntime(payload: SimRuntimeMetricsPayload) {
                        dummyPublishCount.incrementAndGet()
                    }
                }
            )
            simulatorMetricsActive.set(true)
            startWorkerIfNeeded()
            return
        }
        if (serverName.isBlank()) {
            Eln.logger.warn("[Metrics] integrations.mqtt.simMetrics.enabled is true but integrations.mqtt.simMetrics.server is blank")
            stopWorker()
            return
        }
        // PORT: the MQTT sink comes back with mods.eln.mqtt; until then only the dummy sink exists.
        Eln.logger.warn("[Metrics] MQTT sim metrics ($streamId) are not ported to 1.20.1 yet")
        stopWorker()
    }

    @JvmStatic
    fun isSimulatorMetricsActive(): Boolean {
        return simulatorMetricsActive.get()
    }

    @JvmStatic
    fun publishMnaMetrics(
        subSystemCount: Int,
        inversionCount: Int,
        singularMatrixCount: Int,
        inversionAverageNanoseconds: Double,
        inversionMaximumNanoseconds: Long
    ) {
        if (!Eln.simMetricsEnabled || sinks.isEmpty()) {
            return
        }
        queue.offer(
            MnaPayload(
                SimMnaMetricsPayload(
                subSystemCount = subSystemCount,
                inversionCount = inversionCount,
                singularMatrixCount = singularMatrixCount,
                inversionAverageNanoseconds = inversionAverageNanoseconds,
                inversionMaximumNanoseconds = inversionMaximumNanoseconds
                )
            )
        )
        startWorkerIfNeeded()
    }

    @JvmStatic
    fun publishSimulatorRuntimeMetrics(
        avgTickMicroseconds: Double,
        electricalMicroseconds: Double,
        thermalFastMicroseconds: Double,
        thermalSlowMicroseconds: Double,
        slowMicroseconds: Double,
        subSystemCount: Int,
        electricalProcessCount: Int,
        thermalFastLoadCount: Int,
        thermalFastConnectionCount: Int,
        thermalFastProcessCount: Int,
        thermalSlowLoadCount: Int,
        thermalSlowConnectionCount: Int,
        thermalSlowProcessCount: Int,
        slowProcessCount: Int
    ) {
        if (!Eln.simMetricsEnabled || sinks.isEmpty()) {
            return
        }
        queue.offer(
            RuntimePayload(
                SimRuntimeMetricsPayload(
                    avgTickMicroseconds = avgTickMicroseconds,
                    electricalMicroseconds = electricalMicroseconds,
                    thermalFastMicroseconds = thermalFastMicroseconds,
                    thermalSlowMicroseconds = thermalSlowMicroseconds,
                    slowMicroseconds = slowMicroseconds,
                    subSystemCount = subSystemCount,
                    electricalProcessCount = electricalProcessCount,
                    thermalFastLoadCount = thermalFastLoadCount,
                    thermalFastConnectionCount = thermalFastConnectionCount,
                    thermalFastProcessCount = thermalFastProcessCount,
                    thermalSlowLoadCount = thermalSlowLoadCount,
                    thermalSlowConnectionCount = thermalSlowConnectionCount,
                    thermalSlowProcessCount = thermalSlowProcessCount,
                    slowProcessCount = slowProcessCount
                )
            )
        )
        startWorkerIfNeeded()
    }

    @JvmStatic
    @Synchronized
    fun shutdown() {
        simulatorMetricsActive.set(false)
        stopWorker()
        queue.clear()
        sinks.clear()
    }

    @JvmStatic
    fun getDummyPublishCountForTests(): Int {
        return dummyPublishCount.get()
    }

    @JvmStatic
    fun getDummyInversionCountForTests(): Int {
        return dummyInversionCount.get()
    }

    @JvmStatic
    fun getDummySingularCountForTests(): Int {
        return dummySingularCount.get()
    }

    @Synchronized
    private fun stopWorker() {
        running.set(false)
        worker?.interrupt()
        worker = null
    }

    @Synchronized
    private fun startWorkerIfNeeded() {
        if (running.get()) {
            return
        }
        running.set(true)
        worker = Thread(
            {
                while (running.get()) {
                    val payload = queue.poll()
                    if (payload == null) {
                        try {
                            Thread.sleep(50)
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            break
                        }
                        continue
                    }
                    sinks.forEach { sink ->
                        try {
                            when (payload) {
                                is MnaPayload -> sink.publishMna(payload.payload)
                                is RuntimePayload -> sink.publishRuntime(payload.payload)
                            }
                        } catch (e: Exception) {
                            Eln.logger.warn("[Metrics] Sink publish failed: ${e.message}")
                        }
                    }
                }
            },
            "eln-sim-metrics"
        ).apply {
            isDaemon = true
            start()
        }
    }
}
