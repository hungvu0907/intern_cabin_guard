class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val sensorEngine =
            CabinSensorEngine()

        lifecycleScope.launch {

            sensorEngine
                .observeTelemetry()
                .collect { telemetry ->

                    Log.d(
                        "CabinGuard",
                        telemetry.toString()
                    )
                }
        }
    }
}