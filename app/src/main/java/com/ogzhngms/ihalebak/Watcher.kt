package com.ogzhngms.ihalebak

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.IOException
import java.util.concurrent.TimeUnit

// Open tenders in a watched province that are new since the last look. The first look only records what is there,
// so turning a province on does not announce everything it already has.
fun newTenders(current: List<Tender>, seen: Set<String>?, categories: Set<Category>): List<Tender> {
    if (seen == null) return emptyList()
    return current.filter { !it.cancelled && it.ikn !in seen && (categories.isEmpty() || it.category in categories) }
}

// Every few hours, with a network, reads each watched province and posts one notification per province with
// new tenders. The data changes twice a day, so this is never more than a few hours late, and needs no server.
class WatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = Settings(applicationContext)
        val repository = Repository(applicationContext)
        val names = runCatching { repository.index(refresh = true).value.provinces.associate { it.slug to it.name } }.getOrDefault(emptyMap())
        var failed = false
        val news = mutableListOf<News>()
        for (slug in settings.watchedProvinces) {
            val tenders = try {
                repository.province(slug, refresh = true).value
            } catch (e: IOException) {
                failed = true
                continue
            }
            val fresh = newTenders(tenders, settings.seen(slug), settings.categories)
            settings.markSeen(slug, tenders.map { it.ikn }.toSet())
            if (fresh.isNotEmpty()) news += News(slug, names[slug] ?: slug, fresh)
        }
        // A notification per province is right for a few; with many provinces chosen it would be a flood.
        if (news.size <= SEPARATE_NOTIFICATIONS) news.forEach { notify(applicationContext, it.slug, it.province, it.tenders) }
        else notifySummary(applicationContext, news)
        return if (failed) Result.retry() else Result.success()
    }

    companion object {
        private const val NAME = "watch"

        // Starts or stops the periodic check to match the watch list.
        fun schedule(context: Context, settings: Settings) {
            val work = WorkManager.getInstance(context)
            if (settings.watchedProvinces.isEmpty()) {
                work.cancelUniqueWork(NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<WatchWorker>(4, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            work.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

private const val CHANNEL = "new_tenders"
private const val SEPARATE_NOTIFICATIONS = 3
private const val SUMMARY_ID = 1

// The new tenders found in one province.
private class News(val slug: String, val province: String, val tenders: List<Tender>)

fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(CHANNEL, "Yeni ihaleler", NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "Seçtiğin şehirlerde yeni ihale çıkınca"
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

fun canNotify(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

// "İzmir: 3 yeni ihale", opening the agenda on that province.
private fun notify(context: Context, slug: String, province: String, tenders: List<Tender>) {
    val open = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_PROVINCE, slug)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val title = if (tenders.size == 1) "$province: yeni ihale" else "$province: ${tenders.size} yeni ihale"
    post(context, slug.hashCode(), open, title, tenders.take(5).map { it.title })
}

// "12 şehirde 85 yeni ihale", with a line per province, opening the whole agenda.
private fun notifySummary(context: Context, news: List<News>) {
    val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val busiest = news.sortedByDescending { it.tenders.size }
    post(context, SUMMARY_ID, open, "${news.size} şehirde ${news.sumOf { it.tenders.size }} yeni ihale", busiest.take(5).map { "${it.province}: ${it.tenders.size} yeni ihale" })
}

private fun post(context: Context, id: Int, open: Intent, title: String, lines: List<String>) {
    if (!canNotify(context)) return
    val pending = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val notification = NotificationCompat.Builder(context, CHANNEL)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(lines.first())
        .setStyle(NotificationCompat.InboxStyle().also { style -> lines.forEach(style::addLine) })
        .setContentIntent(pending)
        .setAutoCancel(true)
        .build()
    try {
        NotificationManagerCompat.from(context).notify(id, notification)
    } catch (e: SecurityException) {
        // The permission was revoked between the check and the post; nothing to do.
    }
}
