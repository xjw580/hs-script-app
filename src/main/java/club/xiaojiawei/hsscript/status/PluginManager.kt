package club.xiaojiawei.hsscript.status

import club.xiaojiawei.hsscript.bean.Release
import club.xiaojiawei.hsscript.consts.PLUGIN_PATH
import club.xiaojiawei.hsscript.utils.ClassLoaderUtil
import club.xiaojiawei.hsscript.utils.ConfigExUtil
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptcardsdk.CardAction
import club.xiaojiawei.hsscriptcardsdk.CardPlugin
import club.xiaojiawei.hsscriptpluginsdk.Plugin
import club.xiaojiawei.hsscriptpluginsdk.bean.PluginWrapper
import club.xiaojiawei.hsscriptpluginsdk.config.PluginScope
import club.xiaojiawei.hsscriptstrategysdk.DeckStrategy
import club.xiaojiawei.hsscriptstrategysdk.StrategyPlugin
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import java.io.File
import java.util.*
import java.util.stream.StreamSupport


/**
 * @author 肖嘉威
 * @date 2024/9/7 15:05
 */
object PluginManager {

    private data class PendingPlugin<T>(
        val pluginWrapper: PluginWrapper<T>,
        val targetIds: List<String>,
    )

    /**
     * key：pluginId
     */
    val CARD_ACTION_PLUGINS: MutableMap<String, MutableList<PluginWrapper<CardAction>>> = mutableMapOf()

    val DECK_STRATEGY_PLUGINS: MutableMap<String, MutableList<PluginWrapper<DeckStrategy>>> = mutableMapOf()

    private val loadDeck = ReadOnlyBooleanWrapper(false)

    private val loadCard = ReadOnlyBooleanWrapper(false)

    fun isLoadDeck(): Boolean {
        return loadDeck.get()
    }

    fun loadDeckProperty(): ReadOnlyBooleanProperty {
        return loadDeck.readOnlyProperty
    }

    fun isLoadCard(): Boolean {
        return loadCard.get()
    }

    fun loadCardProperty(): ReadOnlyBooleanProperty {
        return loadCard.readOnlyProperty
    }

    fun loadAllPlugins() {
        loadCardPlugin()
        loadDeckPlugin()
    }

    private fun loadDeckPlugin() {
        DeckStrategyManager
        loadDeck.set(false)
        loadPlugin(DeckStrategy::class.java, StrategyPlugin::class.java, DECK_STRATEGY_PLUGINS)
        loadDeck.set(true)
    }

    private fun loadCardPlugin() {
        CardActionManager
        loadCard.set(false)
        loadPlugin(CardAction::class.java, CardPlugin::class.java, CARD_ACTION_PLUGINS)
        loadCard.set(true)
    }

    private val pluginDir by lazy {
        File(PLUGIN_PATH).apply {
            if (!exists()) {
                log.info { "插件目录不存在：${toString()}" }
            }
        }
    }

    private fun <T, P : Plugin> loadPlugin(
        aClass: Class<T>,
        pluginClass: Class<P>,
        pluginWrapperMap: MutableMap<String, MutableList<PluginWrapper<T>>>
    ) {
        pluginWrapperMap.clear()
        val result = ClassLoaderUtil.getClassLoader(pluginDir)
        val pendingPlugins = mutableListOf<PendingPlugin<T>>()

        val deckClassLoaders = result.getOrDefault(emptyList())

        var pluginWrapper: PluginWrapper<T>
        val disableSet: MutableSet<String> =
            if (pluginClass == CardPlugin::class.java) {
                ConfigExUtil.getCardPluginDisabled()
            } else {
                ConfigExUtil.getDeckPluginDisabled()
            }.toMutableSet()
        disableSet.removeAll { it.trim().isEmpty() }

        //        加载内部spi
        val basePlugin = StreamSupport.stream(
            ServiceLoader.load(
                pluginClass,
                PluginManager::class.java.classLoader
            ).spliterator(), false
        ).toList()
        val innerAllInstance: List<T>?
        if (basePlugin.isEmpty()) {
            innerAllInstance = null
        } else {
            innerAllInstance = StreamSupport.stream(
                ServiceLoader.load(aClass, PluginManager::class.java.classLoader).spliterator(),
                false
            ).toList()
            for (p in basePlugin) {
                val isEnabled = !disableSet.contains(p.id())
                if (isEnabled) {
                    p.init()
                }
                val packageName = p::class.java.packageName
                pluginWrapper = PluginWrapper(p, innerAllInstance.filter {
                    it::class.java.packageName.startsWith(packageName)
                })
                pluginWrapper.setEnabled(isEnabled)
                pendingPlugins.add(PendingPlugin(pluginWrapper, resolveTargetIds(pluginWrapper.plugin)))
            }
        }

        //        加载外部spi
        for (deckClassLoader in deckClassLoaders) {
            try {
                val plugins = ArrayList(
                    StreamSupport.stream(
                        ServiceLoader.load(
                            pluginClass, deckClassLoader
                        ).spliterator(), false
                    ).toList()
                )
                if (plugins.isNotEmpty()) {
                    val plugin = plugins.last()
                    val isEnabled = !disableSet.contains(plugin.id())
                    if (isEnabled) {
                        plugin.init()
                    }

                    var stream = StreamSupport.stream(ServiceLoader.load(aClass, deckClassLoader).spliterator(), false)
                    innerAllInstance?.let {
                        stream = stream.filter { i: T ->
                            for (t in innerAllInstance) {
                                if (t!!::class.java.name == i!!::class.java.name) {
                                    return@filter false
                                }
                            }
                            true
                        }
                    }
                    val spiList = stream.toList()
                    if (spiList.isEmpty()) continue
                    pluginWrapper = PluginWrapper(plugin, spiList)
                    pluginWrapper.setEnabled(isEnabled)
                    pendingPlugins.add(PendingPlugin(pluginWrapper, resolveTargetIds(plugin)))
                }
            } catch (e: ServiceConfigurationError) {
                log.warn(e) { "加载SPI错误" }
            } catch (e: Error) {
                log.warn(e) { "加载插件错误" }
            } catch (e: Exception) {
                log.warn(e) { "加载插件错误" }
            }
        }

        applyHighestVersionPolicy(pendingPlugins, pluginWrapperMap, pluginClass.simpleName)
    }

    private fun <T> applyHighestVersionPolicy(
        pendingPlugins: List<PendingPlugin<T>>,
        pluginWrapperMap: MutableMap<String, MutableList<PluginWrapper<T>>>,
        type: String,
    ) {
        val latestPluginMap = pendingPlugins
            .groupBy { it.pluginWrapper.plugin.id() }
            .mapValues { (_, plugins) ->
                plugins.maxWith { left, right ->
                    Release.compareVersion(
                        left.pluginWrapper.plugin.version(),
                        right.pluginWrapper.plugin.version(),
                    )
                }
            }
        for (pendingPlugin in pendingPlugins) {
            val pluginWrapper = pendingPlugin.pluginWrapper
            val plugin = pluginWrapper.plugin
            if (latestPluginMap[plugin.id()] !== pendingPlugin) {
                log.info {
                    "插件${plugin.name()}(${plugin.id()})版本${plugin.version()}不是最高版本，跳过加载"
                }
                continue
            }
            for (targetId in pendingPlugin.targetIds) {
                addPluginWrapper(pluginWrapper, pluginWrapperMap, targetId, type)
            }
        }
    }

    private fun resolveTargetIds(plugin: Plugin): List<String> {
        return if (plugin is CardPlugin) {
            val pluginScope = plugin.pluginScope()
            if (pluginScope === PluginScope.PUBLIC) {
                listOf("")
            } else if (pluginScope === PluginScope.PROTECTED) {
                listOf(plugin.id())
            } else {
                pluginScope.toList()
            }
        } else if (plugin is StrategyPlugin) {
            listOf(plugin.id())
        } else {
            emptyList()
        }
    }

    private fun <T> addPluginWrapper(
        pluginWrapper: PluginWrapper<T>,
        pluginWrapperMap: MutableMap<String, MutableList<PluginWrapper<T>>>,
        pluginId: String, type: String
    ) {
        var pluginWrapperList = pluginWrapperMap[pluginId]
        if (pluginWrapperList == null) {
            pluginWrapperList = mutableListOf(pluginWrapper)
            pluginWrapperMap[pluginId] = pluginWrapperList
        } else {
            pluginWrapperList.add(pluginWrapper)
        }
        pluginWrapper.plugin.apply {
            log.info { "加载${type}：{name: ${name()}, version: ${version()}, author: ${author()}, id: ${id()}, description: ${description()}}" }
        }
    }

    private fun equalsPlugin(plugin1: Plugin?, plugin2: Plugin?): Boolean {
        if (plugin1 == null || plugin2 == null) {
            return false
        }
        return plugin1.id() == plugin2.id()
    }
}
