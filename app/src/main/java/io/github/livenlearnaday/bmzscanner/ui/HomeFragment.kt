package io.github.livenlearnaday.bmzscanner.ui


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import io.github.livenlearnaday.bmzscanner.R
import io.github.livenlearnaday.bmzscanner.databinding.FragmentHomeBinding
import io.github.livenlearnaday.bmzscanner.scanning.ScannerViewModel


class HomeFragment : Fragment(R.layout.fragment_home) {

    private var isQrLogin: Boolean = false


    private var mInstance: HomeFragment? = null
    private var mCtx: Context? = null


    var scannerViewModel: ScannerViewModel? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        super.onCreateView(inflater, container, savedInstanceState)

        val mainview: View = inflater.inflate(R.layout.fragment_home, null)

        scannerViewModel = ViewModelProviders.of(this).get(ScannerViewModel::class.java)

// //To delete all previous scanned data at the start of app
//        if (AppDatabase.getInstance(activity).codeDetailDao() != null){
//
//            scannerViewModel!!.deleteAllCodeDetail()
//
//        }


        return mainview

    }


    private var binding: FragmentHomeBinding? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentHomeBinding.bind(view)


        binding!!.qrLoginBtn.setOnClickListener {


            findNavController().navigate(R.id.navigate_from_home_fragment_to_codeDetectActivity)


        }


    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


}