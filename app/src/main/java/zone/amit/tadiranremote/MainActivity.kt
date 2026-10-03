package zone.amit.tadiranremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private val vm: RemoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var lab by rememberSaveable { mutableStateOf(false) }
            AppTheme {
                if (lab) LabScreen(vm, onBack = { lab = false })
                else RemoteScreen(vm, onOpenLab = { lab = true })
            }
        }
    }
}
