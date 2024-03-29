package dev.justme.busket.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast.LENGTH_LONG
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.justme.busket.R
import dev.justme.busket.databinding.FragmentRegisterBinding
import dev.justme.busket.feathers.FeathersService
import dev.justme.busket.feathers.FeathersSocket
import org.json.JSONObject

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val mainThread = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerRegisterButton.setOnClickListener {
            if (validateInput(
                    binding.registerNameInput.text,
                    binding.registerEmailInput.text,
                    binding.registerPasswordInput.text
                )
            ) {
                if (context == null) return@setOnClickListener
                setButtonsEnabled(false)

                val feathers = FeathersSocket.getInstance(requireContext())

                val obj = mapOf(
                    "fullName" to binding.registerNameInput.text.toString(),
                    "email" to binding.registerEmailInput.text.toString(),
                    "password" to binding.registerPasswordInput.text.toString(),
                )

                feathers.service(FeathersService.Service.USERS).create(JSONObject(obj)) { _, err ->
                    if (err != null) {
                        mainThread.post {
                            Snackbar.make(binding.root, R.string.user_already_exists, LENGTH_LONG).show()
                            setButtonsEnabled(true)
                        }
                        return@create
                    }
                    feathers.authenticate(obj["email"] ?: "", obj["password"] ?: "", {
                        mainThread.post {
                            findNavController().navigate(R.id.action_RegisterFragment_to_HomeFragment)
                        }
                    })
                }
            }
        }

        binding.registerLoginButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.registerRegisterButton.isEnabled = enabled
        binding.registerLoginButton.isEnabled = enabled
    }

    private fun validateInput(name: Editable?, email: Editable?, password: Editable?): Boolean {
        if (name != null) {
            if (name.length < 3) {
                binding.registerNameInput.error = "Name has to be at least 3 characters long"
                return false
            } else if (name.length >= 16) {
                binding.registerNameInput.error = "Name mustn't be longer than 16 characters"
                return false
            }
        }

        if (email != null) {
            if (email.isEmpty()) {
                binding.registerEmailInput.error = "Email mustn't be blank"
                return false
            }

            if (!email.contains("@") || !email.contains(".")) {
                binding.registerEmailInput.error = "Invalid email"
                return false
            }
        }

        if (password != null) {
            if (password.length < 3) {
                binding.registerPasswordInput.error = "Password should be longer than 3 characters"
                return false
            }
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}