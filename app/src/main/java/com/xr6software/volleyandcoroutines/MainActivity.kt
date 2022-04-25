package com.xr6software.volleyandcoroutines

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

class MainActivity : AppCompatActivity() {

    private val RESULT_1 = "Result #1"
    private val RESULT_2 = "Result #2"

    private val PROGRESS_MAX = 100
    private val PROGRESS_MIN = 0
    private val JOB_TIME = 4000

    private lateinit var job: CompletableJob


    //On create method, sets buttons listeners
    /*
    Button example 0 -> fires a method that fakes an API request, in IO scope of coroutines
    Button example 1 -> starts a Coroutine Job, or stops it if its running
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        ma_button_example0.setOnClickListener {

            CoroutineScope(IO).launch {
                fakeApiRequest()
            }

        }

        ma_button_example1.setOnClickListener {

            if (!::job.isInitialized) {
                initJobCoroutine()
            }
            ma_progress_bar.startJobOrCancel(job)

        }

    }

    //Example 0
    //This method waits for 10 seconds and then returns a string.
    //As it runs inside a coroutine, it also must have the SUSPEND
    private suspend fun getResultFromApi(): String {
        logThread("getResultFromAPI")
        delay(10000)
        return RESULT_1
    }

    private suspend fun getResultTwoFromApi(): String {
        logThread("getResultTwoFromApi")
        delay(3000)
        return RESULT_2
    }

    //Fake API request.. SUSPEND is required in order to execute in Coroutine Scope
    /*
    When the getResultFromApi returns the string, it's passed on the Set text on main thread method.
    Then it do the same with the getResult Two from Api.
     */
    private suspend fun fakeApiRequest() {
        var result = getResultFromApi()
        setTextOnMainThread(ma_text_result.text.toString() + "\n${result}")
        result = getResultTwoFromApi()
        setTextOnMainThread(result)
    }

    //This method runs the set new text method inside a coroutine, but in the main Scope, not the IO.
    private suspend fun setTextOnMainThread(input: String) {

        withContext(Main) {
            logThread("setTextOnMainThread")
            setNewText(input)
        }

    }

    //This method set the input string in the textview
    private fun setNewText(input: String) {
        ma_text_result.text = input
    }



    //EXAMPLE 1 ->
    /*
    This example creates a Coroutine Job and runs the job in the IO scope but with a different thread for the job
    this helps to avoid the job cancellation if the whole IO scope is stopped.
     */

    //This method set ups the Job, the progress bar values, and sets the Completion listener
    private fun initJobCoroutine() {

        ma_button_example1.text = "Start job"
        updateJobTextView("")

        job = Job()
        job.invokeOnCompletion {
            it?.message.let {
                var msg = it
                if (msg.isNullOrBlank()) {
                    msg = "Unknown cancelation error"
                }
                println("${job} was canceled. Reason: $msg")
                showToast(msg)
            }
        }
        ma_progress_bar.max = PROGRESS_MAX
        ma_progress_bar.progress = PROGRESS_MIN

    }

    //Extension function for the progress bar
    /*
    This function handles the coroutine launch, acording to if its running already or not.
     */
    private fun ProgressBar.startJobOrCancel(job: Job) {
        if (this.progress > 0) {
            println("this ${job} is already active. Cancelling...")
            resetJob()
        } else {
            ma_button_example1.text = "Cancel Job #1"
            //Complete new scope for Job inside the IO scope
            //To close only the job, you call job.cancel()
            CoroutineScope(IO + job).launch {
                println("corountine ${this} is activated with ${job}")
                for (i in PROGRESS_MIN..PROGRESS_MAX) {
                    delay((JOB_TIME / PROGRESS_MAX).toLong())
                    this@startJobOrCancel.progress = i
                }
                updateJobTextView("Job is Complete")
            }
        }
    }

    //This method sets the "Job is complete" in the textview, it runs in a different scope, the Main scope.
    private fun updateJobTextView(text: String) {
        GlobalScope.launch(Main) {
            ma_text_result.text = text
        }
    }

    private fun resetJob() {
        if (job.isActive || job.isCompleted) {
            job.cancel(CancellationException("Resetting job"))
        }
        initJobCoroutine()
    }

    private fun showToast(text: String) {
        GlobalScope.launch(Main) {
            Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
        }

    }

    //This method Logs the method name and the Thread where it is running.
    private fun logThread(methodName: String) {
        println("debug: ${methodName} : ${Thread.currentThread().name}")
    }

}