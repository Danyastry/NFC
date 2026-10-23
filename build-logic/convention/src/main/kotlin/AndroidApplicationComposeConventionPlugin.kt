import com.android.build.api.dsl.ApplicationExtension
import com.danyastry.nfcpay.buildlogic.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        configureAndroidCompose(extensions.getByType<ApplicationExtension>())
    }
}
