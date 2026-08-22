package com.nuvio.tv.core.sync.iptv

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.nuvio.tv.domain.repository.IptvEpgRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.nuvio.tv.data.local.iptv.IptvPreferencesDataStore

/** Deferred background guide refresh; it never blocks app launch. */
class IptvEpgRefreshJobService : JobService() {
    @dagger.hilt.EntryPoint @InstallIn(SingletonComponent::class)
    interface EpgJobEntryPoint { fun epgRepository(): IptvEpgRepository; fun preferences(): IptvPreferencesDataStore }
    private var scope: CoroutineScope? = null
    override fun onStartJob(params: JobParameters): Boolean {
        val entry = EntryPointAccessors.fromApplication(applicationContext, EpgJobEntryPoint::class.java)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope!!.launch {
            try { if (entry.preferences().epgAutoSyncEnabled.first()) entry.epgRepository().refreshManualSources() }
            finally { jobFinished(params, false) }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { scope?.cancel(); return true }
    companion object {
        private const val JOB_ID = 10_120
        fun schedule(context: Context) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            if (scheduler.allPendingJobs.any { it.id == JOB_ID }) return
            scheduler.schedule(JobInfo.Builder(JOB_ID, ComponentName(context, IptvEpgRefreshJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setPeriodic(24 * 60 * 60 * 1000L).build())
        }
    }
}
